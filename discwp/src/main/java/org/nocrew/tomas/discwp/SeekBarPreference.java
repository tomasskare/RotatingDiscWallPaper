package org.nocrew.tomas.discwp;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {
    
    private final String TAG = getClass().getName();
    
    private static final String ANDROIDNS =
	"http://schemas.android.com/apk/res/android";
    private static final String NOCREWNS =
	"http://nocrew.org";
    private static final int DEFAULT_VALUE = 50;
    
    private int mMaxValue      = 100;
    private int mMinValue      = 0;
    private int mInterval      = 1;
    private int mCurrentValue;
    private String mUnitsLeft  = "";
    private String mUnitsRight = "";
    private SeekBar mSeekBar;

    private Context context;
    private View mLayout;
    private TextView mStatusText;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPreference(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPreference(context, attrs);
    }

    private void initPreference(Context context, AttributeSet attrs) {
	this.context = context;
        setValuesFromXml(attrs);
        mSeekBar = new SeekBar(context, attrs);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(this);
    }
    
    private void setValuesFromXml(AttributeSet attrs) {
        mMaxValue = attrs.getAttributeIntValue(ANDROIDNS, "max", 100);
        mMinValue = attrs.getAttributeIntValue(NOCREWNS, "min", 0);
        
        mUnitsLeft = getAttributeStringValue(attrs, NOCREWNS, "unitsLeft", "");
        String units = getAttributeStringValue(attrs, NOCREWNS, "units", "");
        mUnitsRight = getAttributeStringValue(attrs, NOCREWNS, "unitsRight", units);
        
        try {
            String newInterval = attrs.getAttributeValue(NOCREWNS, "interval");
            if(newInterval != null)
                mInterval = Integer.parseInt(newInterval);
        }
        catch(Exception e) {
            Log.e(TAG, "Invalid interval value", e);
        }
        
    }
    
    private String getAttributeStringValue(AttributeSet attrs, String namespace, String name, String defaultValue) {
        String value = attrs.getAttributeValue(namespace, name);
        if(value == null)
            value = defaultValue;
        
        return value;
    }
    
    @Override
    protected View onCreateView(ViewGroup parent){
        
        RelativeLayout layout =  null;
        
        try {
            LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            layout = (RelativeLayout)mInflater.inflate(R.layout.seek_bar_preference, parent, false);
	    TextView text = (TextView)layout.findViewById(android.R.id.title);
	    text.setEnabled(isEnabled());
	    if(isEnabled())
		text.setTextColor(context.getResources().getColor(R.color.normal_color));
	    else
		text.setTextColor(context.getResources().getColor(R.color.disabled_color));
        }
        catch(Exception e)
        {
            Log.e(TAG, "Error creating seek bar preference", e);
        }

	mLayout = layout;

	return layout;
    }
    
    @Override
    public void onBindView(View view) {
        super.onBindView(view);

        try
        {
            // move our seekbar to the new view we've been given
            ViewParent oldContainer = mSeekBar.getParent();
            ViewGroup newContainer = (ViewGroup) view.findViewById(R.id.seekBarPrefBarContainer);
            
            if (oldContainer != newContainer) {
                // remove the seekbar from the old view
                if (oldContainer != null) {
                    ((ViewGroup) oldContainer).removeView(mSeekBar);
                }
                // remove the existing seekbar (there may not be one) and add ours
                newContainer.removeAllViews();
                newContainer.addView(mSeekBar, ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
        catch(Exception ex) {
            Log.e(TAG, "Error binding view: " + ex.toString());
        }

        updateView(view);
    }
    
    /**
     * Update a SeekBarPreference view with our current state
     * @param view
     */
    protected void updateView(View view) {

        try {
            RelativeLayout layout = (RelativeLayout)view;

            mStatusText = (TextView)layout.findViewById(R.id.seekBarPrefValue);
            mStatusText.setText(String.valueOf(mCurrentValue));
            mStatusText.setMinimumWidth(30);
            
            mSeekBar.setProgress(mCurrentValue - mMinValue);

        }
        catch(Exception e) {
            Log.e(TAG, "Error updating seek bar preference", e);
        }
        
    }

    @Override
    public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);

	mSeekBar.setEnabled(enabled);

	if(mLayout != null) {
	    TextView text = (TextView)mLayout.findViewById(android.R.id.title);
	    text.setEnabled(enabled);
	    if(enabled)
		text.setTextColor(context.getResources().getColor(R.color.normal_color));
	    else
		text.setTextColor(context.getResources().getColor(R.color.disabled_color));
	}
    }
    
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int newValue = progress + mMinValue;
        
        if(newValue > mMaxValue)
            newValue = mMaxValue;
        else if(newValue < mMinValue)
            newValue = mMinValue;
        else if(mInterval != 1 && newValue % mInterval != 0)
            newValue = Math.round(((float)newValue)/mInterval)*mInterval;  
        
        // change rejected, revert to the previous value
        if(!callChangeListener(newValue)){
            seekBar.setProgress(mCurrentValue - mMinValue); 
            return; 
        }

        // change accepted, store it
        mCurrentValue = newValue;
        mStatusText.setText(String.valueOf(newValue));
        persistInt(newValue);

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        notifyChanged();
    }


    @Override 
    protected Object onGetDefaultValue(TypedArray ta, int index){
        
        int defaultValue = ta.getInt(index, DEFAULT_VALUE);
        return defaultValue;
        
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

        if(restoreValue) {
            mCurrentValue = getPersistedInt(mCurrentValue);
        }
        else {
            int temp = 0;
            try {
                temp = (Integer)defaultValue;
            }
            catch(Exception ex) {
                Log.e(TAG, "Invalid default value: " + defaultValue.toString());
            }
            
            persistInt(temp);
            mCurrentValue = temp;
        }
        
    }
    
}