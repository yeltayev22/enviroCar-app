package org.envirocar.obd.adapter;

import org.envirocar.core.logging.Logger;
import org.envirocar.obd.commands.request.BasicCommand;
import org.envirocar.obd.commands.request.PIDCommand;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.PIDUtil;
import org.envirocar.obd.exception.AdapterSearchingException;
import org.envirocar.obd.exception.NoDataReceivedException;
import org.envirocar.obd.exception.StreamFinishedException;
import org.envirocar.obd.exception.UnmatchedResponseException;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.ResponseParser;
import org.envirocar.obd.exception.AdapterFailedException;
import org.envirocar.obd.exception.InvalidCommandResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

public abstract class SyncAdapter implements OBDAdapter {

    private static final Logger LOGGER = Logger.getLogger(SyncAdapter.class.getName());
    private static final char COMMAND_SEND_END = '\r';
    private static final char COMMAND_RECEIVE_END = '>';
    private static final char COMMAND_RECEIVE_SPACE = ' ';
    private static final int MAX_ERROR_PER_COMMAND = 5;

    private Set<Character> ignoredChars = new HashSet<>(Arrays.asList(COMMAND_RECEIVE_SPACE, COMMAND_SEND_END));
    private CommandExecutor commandExecutor;
    private ResponseParser parser = new ResponseParser();

    private Map<PID, AtomicInteger> failureMap = new HashMap<>();
    private List<PIDCommand> requestCommands;
    private Queue<PIDCommand> commandRingBuffer = new ArrayDeque<>();

    @Override
    public Observable<Boolean> initialize(InputStream is, OutputStream os) {
        commandExecutor = new CommandExecutor(is, os, ignoredChars, COMMAND_RECEIVE_END, COMMAND_SEND_END);

        /**
         * create an observable that tries to verify the
         * connection based on response analysis
         */
        Observable<Boolean> obs = Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    while (!subscriber.isUnsubscribed()) {
                        BasicCommand cc = pollNextInitializationCommand();

                        if (cc == null) {
                            subscriber.onError(new AdapterFailedException(
                                    "All init commands sent, but could not verify connection"));
                            subscriber.unsubscribe();
                        }

                        //push the command to the output stream
                        commandExecutor.execute(cc);

                        //check if the command needs a response (most likely)
                        if (cc.awaitsResults()) {
                            byte[] resp = commandExecutor.retrieveLatestResponse();

                            if (analyzeMetadataResponse(resp, cc)) {
                                //the impl decided that it can support this kind of data stream
                                subscriber.onNext(true);
                                subscriber.unsubscribe();
                                break;
                            }
                        }
                    }
                } catch (IOException | AdapterFailedException e) {
                    subscriber.onError(e);
                    subscriber.unsubscribe();
                } catch (StreamFinishedException e) {
                    subscriber.onError(new IOException("The stream was closed unexpectedly"));
                    subscriber.unsubscribe();
                }
            }
        });

        return obs;
    }

    @Override
    public Observable<DataResponse> observe() {
        return observe(Schedulers.io());
    }

    /**
     * start the actual data observable
     *
     * @param subscriberScheduler the Scheduler to be used
     * @return the data observable
     */
    protected Observable<DataResponse> observe(Scheduler subscriberScheduler) {
        final Scheduler usedSubScheduler = subscriberScheduler == null ? Schedulers.io() : subscriberScheduler;

        return Observable.create(new Observable.OnSubscribe<DataResponse>() {

            @Override
            public void call(Subscriber<? super DataResponse> subscriber) {
                /**
                 * create an observable that provide raw bytes of the input stream
                 * and subscribe an observer to it that parses the bytes
                 */
                Subscription obs = commandExecutor.createRawByteObservable()
                        //subscribe (= where the i/o with the streams is done) on this scheduler:
                        .subscribeOn(usedSubScheduler)
                        //observe (= where onNext is called) on this scheduler:
                        .observeOn(usedSubScheduler)
                        .subscribe(new Observer<byte[]>() {
                            private PIDCommand latestCommand;

                            @Override
                            public void onCompleted() {
                                subscriber.onCompleted();
                                subscriber.unsubscribe();
                            }

                            @Override
                            public void onError(Throwable e) {
                                //TODO switch over exceptions?
                                if (e instanceof StreamFinishedException) {
                                    subscriber.onCompleted();
                                }
                                else {
                                    subscriber.onError(e);
                                }

                                subscriber.unsubscribe();
                            }

                            @Override
                            public void onNext(byte[] bytes) {
                                if (subscriber.isUnsubscribed()) {
                                    // we do not push more commands into the socket --> no more onNext calls for us
                                    return;
                                }

                                try {
                                    DataResponse response = parser.parse(preProcess(bytes));

                                    if (response != null) {
                                        subscriber.onNext(response);
                                    }
                                } catch (AdapterSearchingException e) {
                                    LOGGER.warn("Adapter still searching: " + e.getMessage());
                                } catch (NoDataReceivedException e) {
                                    LOGGER.warn("No data received: " + e.getMessage());
                                    increaseFailureCount(latestCommand.getPid());
                                } catch (InvalidCommandResponseException e) {
                                    increaseFailureCount(PIDUtil.fromString(e.getCommand()));
                                } catch (UnmatchedResponseException e) {
                                    LOGGER.warn("Unmatched response: " + e.getMessage());
                                }

                                /**
                                 * poll the next command and push it to the adapter
                                 */
                                try {
                                    latestCommand = pollNextCommand();
                                    LOGGER.debug("Sending command " + latestCommand != null ? latestCommand.getPid().toString() : "n/a");
                                    commandExecutor.execute(latestCommand);
                                } catch (AdapterFailedException | IOException e) {
                                    subscriber.onError(e);
                                    subscriber.unsubscribe();
                                }

                            }
                        });
                subscriber.add(obs);

                preparePendingCommands();

                try {
                    commandExecutor.execute(pollNextCommand());
                } catch (IOException e) {
                    subscriber.onError(e);
                    subscriber.unsubscribe();
                } catch (AdapterFailedException e) {
                    subscriber.onError(e);
                    subscriber.unsubscribe();
                }

            }
        });
    }

    protected PIDCommand pollNextCommand() throws AdapterFailedException {
        if (this.commandRingBuffer.isEmpty()) {
            throw new AdapterFailedException("No available commands left in the buffer");
        }

        PIDCommand cmd = commandRingBuffer.poll();

        if (cmd != null) {
            if (!checkIsBlacklisted(cmd.getPid())) {
                /**
                 * not blacklisted: add it back as the last element of the ring
                 */
                commandRingBuffer.offer(cmd);
                return cmd;
            }
            else {
                /**
                 * blacklisted: do not re-add it and return the next candidate
                 */
                return pollNextCommand();
            }
        }

        return cmd;
    }

    protected void increaseFailureCount(PID command) {
        if (command == null) {
            return;
        }

        if (this.failureMap.containsKey(command)) {
            this.failureMap.get(command).getAndIncrement();
        }
        else {
            AtomicInteger ai = new AtomicInteger(1);
            this.failureMap.put(command, ai);
        }
    }

    protected List<PIDCommand> defaultCycleCommands() {
        if (requestCommands == null) {
            requestCommands = new ArrayList<>();

            requestCommands.add(PIDUtil.instantiateCommand(PID.SPEED));
            requestCommands.add(PIDUtil.instantiateCommand(PID.MAF));
            requestCommands.add(PIDUtil.instantiateCommand(PID.RPM));
            requestCommands.add(PIDUtil.instantiateCommand(PID.INTAKE_MAP));
            requestCommands.add(PIDUtil.instantiateCommand(PID.INTAKE_AIR_TEMP));
            requestCommands.add(PIDUtil.instantiateCommand(PID.CALCULATED_ENGINE_LOAD));
            requestCommands.add(PIDUtil.instantiateCommand(PID.TPS));
            requestCommands.add(PIDUtil.instantiateCommand(PID.O2_LAMBDA_PROBE_1_VOLTAGE));
            requestCommands.add(PIDUtil.instantiateCommand(PID.O2_LAMBDA_PROBE_1_CURRENT));
        }

        return requestCommands;
    }

    private void preparePendingCommands() {
        commandRingBuffer = new ArrayDeque<>();

        for (PIDCommand cmd : providePendingCommands()) {
            if (cmd != null) {
                commandRingBuffer.offer(cmd);
            }
        }
    }

    private boolean checkIsBlacklisted(PID pid) {
        return this.failureMap.containsKey(pid)  && this.failureMap.get(pid).get() > MAX_ERROR_PER_COMMAND;
    }

    protected abstract BasicCommand pollNextInitializationCommand();

    protected abstract List<PIDCommand> providePendingCommands();

    /**
     *
     * @param response the raw data received
     * @param sentCommand the originating command
     * @return true if the adapter established a meaningful connection
     */
    protected abstract boolean analyzeMetadataResponse(byte[] response, BasicCommand sentCommand) throws AdapterFailedException;

    protected abstract byte[] preProcess(byte[] bytes);
}
