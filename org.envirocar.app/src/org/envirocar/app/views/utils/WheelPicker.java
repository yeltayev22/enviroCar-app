package org.envirocar.app.views.utils;

import android.graphics.Typeface;

import java.util.List;

/**
 * Interface of WheelPicker
 *
 * @author AigeStudio
 * https://github.com/AigeStudio/WheelPicker
 */
public interface WheelPicker {

    /**
     * Get the count of current visible items in WheelPicker
     */
    int getVisibleItemCount();

    /**
     * Set the count of current visible items in WheelPicker
     * The count of current visible items in WheelPicker must greater than 1
     * Notice:count of current visible items in WheelPicker will always is an odd number, even you
     * can set an even number for it, it will be change to an odd number eventually
     * By default, the count of current visible items in WheelPicker is 7
     */
    void setVisibleItemCount(int count);

    /**
     * Whether WheelPicker is cyclic or not
     */
    boolean isCyclic();

    /**
     * Set whether WheelPicker is cyclic or not
     * WheelPicker's items will be end to end and in an infinite loop if setCyclic true, and there
     * is no border whit scroll when WheelPicker in cyclic state
     */
    void setCyclic(boolean isCyclic);

    void setOnItemSelectedListener(WheelPickerView.OnItemSelectedListener listener);

    /**
     * Get the position of current selected item in data source
     * Notice:The value by return will not change when WheelPicker scroll, this method will always
     * return the value which {@link #setSelectedItemPosition(int)} set, the value this method
     * return will be changed if and only if call the
     * {@link #setSelectedItemPosition(int)}
     * set a new value
     * If you only want to get the position of current selected item in data source, you can get it
     * through {@link WheelPickerView.OnItemSelectedListener} or call
     * {@link #getCurrentItemPosition()} directly
     */
    int getSelectedItemPosition();

    /**
     * Set the position of current selected item in data source
     * Call this method and set a new value may be reinitialize the location of WheelPicker. For
     * example, you call this method after scroll the WheelPicker and set selected item position
     * with a new value, WheelPicker will clear the related parameters last scroll set and reset
     * series of data, and make the position 3 as a new starting point of WheelPicker, this behavior
     * maybe influenced some attribute you set last time, such as parameters of method in
     * {@link WheelPickerView.OnWheelChangeListener} and
     * {@link WheelPickerView.OnItemSelectedListener}, so you must always
     * consider the influence when you call this method set a new value
     * You should always set a value which greater than or equal to 0 and less than data source's
     * length
     * By default, position of current selected item in data source is 0
     */
    void setSelectedItemPosition(int position);

    /**
     * Get the position of current selected item in data source
     * The difference between {@link #getSelectedItemPosition()}, the value this method return will
     * change by WheelPicker scrolled
     */
    int getCurrentItemPosition();

    /**
     * Get data source of WheelPicker
     */
    List getData();

    /**
     * Set data source of WheelPicker
     * The data source can be any type, WheelPicker will change the data to string when it draw the
     * item.
     * There is a default data source when you not set the data source for WheelPicker.
     * Set data source for WheelPicker will reset state of it, you can refer to
     * {@link #setSelectedItemPosition(int)} for more details.
     */
    void setData(List data);

    /**
     * Set items of WheelPicker if has same width
     * WheelPicker will traverse the data source to calculate each data text width to find out the
     * maximum text width for the final view width, this process maybe spends a lot of time and
     * reduce efficiency when data source has large amount data, in most large amount data case,
     * data text always has same width, you can call this method tell to WheelPicker your data
     * source has same width to save time and improve efficiency.
     * Sometimes the data source you set is positively has different text width, but maybe you know
     * the maximum width text's position in data source, then you can call
     * {@link #setMaximumWidthTextPosition(int)} tell to WheelPicker where is the maximum width text
     * in data source, WheelPicker will calculate its width base on this text which found by
     * position. If you don't know the position of maximum width text in data source, but you have
     * maximum width text, you can call {@link #setMaximumWidthText(String)} tell to WheelPicker
     * what maximum width text is directly, WheelPicker will calculate its width base on this text.
     */
    void setSameWidth(boolean hasSameSize);

    /**
     * Whether items has same width or not
     */
    boolean hasSameWidth();

    /**
     * @see WheelPickerView.OnWheelChangeListener
     */
    void setOnWheelChangeListener(WheelPickerView.OnWheelChangeListener listener);

    /**
     * Get maximum width text
     */
    String getMaximumWidthText();

    /**
     * @see #setSameWidth(boolean)
     */
    void setMaximumWidthText(String text);

    /**
     * Get the position of maximum width text in data source
     */
    int getMaximumWidthTextPosition();

    /**
     * Set the position of maximum width text in data source
     * @see #setSameWidth(boolean)
     */
    void setMaximumWidthTextPosition(int position);

    /**
     * Get text color of current selected item
     */
    int getSelectedItemTextColor();

    /**
     * Set text color of current selected item
     */
    void setSelectedItemTextColor(int color);

    /**
     * Get text color of items
     */
    int getItemTextColor();

    /**
     * Set text color of items
     */
    void setItemTextColor(int color);

    /**
     * Get text size of items
     * Unit in px
     */
    int getItemTextSize();

    /**
     * Set text size of items
     * Unit in px
     */
    void setItemTextSize(int size);

    /**
     * Get space between items
     * Unit in px
     */
    int getItemSpace();

    /**
     * Set space between items
     * Unit in px
     */
    void setItemSpace(int space);

    /**
     * Set whether WheelPicker display indicator or not
     * WheelPicker will draw two lines above an below current selected item if display indicator
     * Notice:Indicator's size will not participate in WheelPicker's size calculation, it will drawn
     * above the content
     */
    void setIndicator(boolean hasIndicator);

    /**
     * Whether WheelPicker display indicator or not
     */
    boolean hasIndicator();

    /**
     * Get size of indicator
     * Unit in px
     */
    int getIndicatorSize();

    /**
     * Set size of indicator
     */
    void setIndicatorSize(int size);

    /**
     * Get color of indicator
     */
    int getIndicatorColor();

    /**
     * Set color of indicator
     */
    void setIndicatorColor(int color);

    /**
     * Set whether WheelPicker display curtain or not
     * WheelPicker will draw a rectangle as big as current selected item and fill specify color
     * above content if curtain display
     */
    void setCurtain(boolean hasCurtain);

    /**
     * Whether WheelPicker display curtain or not
     */
    boolean hasCurtain();

    /**
     * Get color of curtain
     */
    int getCurtainColor();

    /**
     * Set color of curtain
     */
    void setCurtainColor(int color);

    /**
     * Set whether WheelPicker has atmospheric or not
     * WheelPicker's items will be transparent from center to ends if atmospheric display
     */
    void setAtmospheric(boolean hasAtmospheric);

    /**
     * Whether WheelPicker has atmospheric or not
     */
    boolean hasAtmospheric();

    /**
     * Whether WheelPicker enable curved effect or not
     */
    boolean isCurved();

    /**
     * Set whether WheelPicker enable curved effect or not
     * If setCurved true, WheelPicker will display with curved effect looks like ends bend into
     * screen with perspective.
     * WheelPicker's curved effect base on strict geometric model, some parameters relate with size
     * maybe invalidated, for example each item size looks like different because of perspective in
     * curved, the space between items looks like have a little difference
     */
    void setCurved(boolean isCurved);

    /**
     * Get alignment of WheelPicker
     */
    int getItemAlign();

    /**
     * Set alignment of WheelPicker
     * The default alignment of WheelPicker is {@link WheelPickerView#ALIGN_CENTER}
     *
     * @param align {@link WheelPickerView#ALIGN_CENTER}
     *              {@link WheelPickerView#ALIGN_LEFT}
     *              {@link WheelPickerView#ALIGN_RIGHT}
     */
    void setItemAlign(int align);

    /**
     * Get typeface of item text
     */
    Typeface getTypeface();

    /**
     * Set typeface of item text
     * Set typeface of item text maybe cause WheelPicker size change
     */
    void setTypeface(Typeface tf);
}