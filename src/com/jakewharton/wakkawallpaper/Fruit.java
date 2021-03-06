package com.jakewharton.wakkawallpaper;

import java.util.LinkedList;
import java.util.List;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

/**
 * The Fruit class is a special reward entity that appears only at specific times.
 * 
 * @author Jake Wharton
 */
public class Fruit extends Entity implements SharedPreferences.OnSharedPreferenceChangeListener {
	/**
	 * The character that represents the fruit.
	 * 
	 * @author Jake Wharton
	 */
	enum Character { FRUIT, GOOGOL }
	
	/**
	 * The type of fruit.
	 * 
	 * @author Jake Wharton
	 */
	enum Type {
		CHERRY(100, new Rect(0, 0, 12, 14)),
		STRAWBERRY(300, new Rect(1, 20, 12, 34)),
		PEACH(500, new Rect(0, 40, 12, 54)),
		APPLE(700, new Rect(0, 60, 12, 74)),
		GRAPES(1000, new Rect(40, 0, 52, 14)),
		GALAXIAN(2000, new Rect(40, 20, 52, 34)),
		BELL(3000, new Rect(40, 40, 52, 54)),
		KEY(5000, new Rect(40, 60, 52, 74));
		
		
		
		/**
		 * Point value of this type of fruit.
		 */
		public final int points;
		
		/**
		 * The location of the sprite for this fruit.
		 */
		public final Rect sprite;
		
		
		
		/**
		 * Create a fruit type.
		 * 
		 * @param points Value of fruit in points.
		 * @param sprite Location of fruit's sprite.
		 */
		private Type(final int points, final Rect sprite) {
			this.points = points;
			this.sprite = sprite;
		}
	}
	
	
	
	/**
	 * Tag used for logging.
	 */
	private static final String TAG = "WakkaWallpaper.Fruit";
	
	
	
	/**
	 * The type of fruit.
	 */
	private Fruit.Type mType;
	
	/**
	 * The character representing this fruit.
	 */
	private Fruit.Character mCharacter;
	
	/**
	 * The time at which the fruit was created on screen.
	 */
	private long mCreated;
	
	/**
	 * Whether or not the fruit is currently visible on screen.
	 */
	private boolean mIsVisible;
	
	/**
	 * The length of time this fruit will be visible on screen.
	 */
	private int mVisibleLength;
	
	/**
	 * The lowest amount of time possible a fruit can appear on the screen.
	 */
	private int mVisibleLower;
	
	/**
	 * The highest amount of time possible a fruit can appear on the screen.
	 */
	private int mVisibleUpper;
	
	/**
	 * The dot threshold at which the first fruit is displayed.
	 */
	private int mThresholdFirst;
	
	/**
	 * The dot threshold at which the second fruit is displayed.
	 */
	private int mThresholdSecond;
	
	/**
	 * The number of fruits that have been displayed so far this level.
	 */
	private int mNumberDisplayed;
	
	/**
	 * Bitmap holding the fruit sprites.
	 */
	private Bitmap mFruits;
	
	/**
	 * List of possible board positions a fruit could appear in.
	 */
	private List<Point> mPositions;
	
	/**
	 * Whether or not the Eden trophy is enabled.
	 */
    private boolean mIsTrophyEdenEnabled;
    
    
	
	/**
	 * Initialize a new fruit adhering to the parameters.
	 */
	public Fruit() {
		super();
		
		//We are fruit. We can't wrap.
		this.mIsWrapping = false;
		
		this.mPositions = new LinkedList<Point>();

        //Load all preferences or their defaults
        Wallpaper.PREFERENCES.registerOnSharedPreferenceChangeListener(this);
        this.onSharedPreferenceChanged(Wallpaper.PREFERENCES, null);
	}

	
	
    /**
     * Handle the changing of a preference.
     */
	public void onSharedPreferenceChanged(final SharedPreferences preferences, final String key) {
		if (Wallpaper.LOG_VERBOSE) {
			Log.v(Fruit.TAG, "> onSharedPreferenceChanged()");
		}
		
		final boolean all = (key == null);
		final Resources resources = Wallpaper.CONTEXT.getResources();
		
		boolean changed = false;
		
		final String thresholdFirst = Wallpaper.CONTEXT.getString(R.string.settings_game_fruitonethreshold_key);
		if (all || key.equals(thresholdFirst)) {
			this.mThresholdFirst = preferences.getInt(key, resources.getInteger(R.integer.game_fruitonethreshold_default));
			changed = true;
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Fruit.TAG, "First Threshold: " + this.mThresholdFirst);
			}
		}

		final String thresholdSecond = Wallpaper.CONTEXT.getString(R.string.settings_game_fruittwothreshold_key);
		if (all || key.equals(thresholdSecond)) {
			this.mThresholdSecond = preferences.getInt(key, resources.getInteger(R.integer.game_fruittwothreshold_default));
			changed = true;
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Fruit.TAG, "Second Threshold: " + this.mThresholdSecond);
			}
		}
		
		final String visibleLower = Wallpaper.CONTEXT.getString(R.string.settings_game_fruitvisiblelower_key);
		if (all || key.equals(visibleLower)) {
			this.mVisibleLower = preferences.getInt(key, resources.getInteger(R.integer.game_fruitvisiblelower_default));
			changed = true;
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Fruit.TAG, "Visible Lower: " + this.mVisibleLower);
			}
		}
		
		final String visibleUpper = Wallpaper.CONTEXT.getString(R.string.settings_game_fruitvisibleupper_key);
		if (all || key.equals(visibleUpper)) {
			this.mVisibleUpper = preferences.getInt(key, resources.getInteger(R.integer.game_fruitvisibleupper_default));
			changed = true;
			
			if (Wallpaper.LOG_DEBUG) {
				Log.d(Fruit.TAG, "Visible Upper: " + this.mVisibleUpper);
			}
		}
		
		final String trophyGoogol = Wallpaper.CONTEXT.getString(R.string.trophy_googol_key);
		if (all || key.equals(trophyGoogol)) {
			final boolean enabled = preferences.getBoolean(trophyGoogol, resources.getBoolean(R.bool.trophy_googol_default));
			
			if (enabled) {
				this.mCharacter = Fruit.Character.GOOGOL;

				//Load the fruit sprites
				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inScaled = false;
				this.mFruits = BitmapFactory.decodeResource(Wallpaper.CONTEXT.getResources(), R.drawable.googol_fruit, options);
			} else {
				this.mCharacter = Fruit.Character.FRUIT;
				
				//Load the fruit sprites
				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inScaled = false;
				this.mFruits = BitmapFactory.decodeResource(Wallpaper.CONTEXT.getResources(), R.drawable.fruits, options);
			}
		}
		
		final String trophyEden = resources.getString(R.string.trophy_eden_key);
		if (all || key.equals(trophyEden)) {
			this.mIsTrophyEdenEnabled = preferences.getBoolean(trophyEden, resources.getBoolean(R.bool.trophy_eden_default));
		}
		
		
		if (changed) {
			this.hide();
		}
        
		if (Wallpaper.LOG_VERBOSE) {
			Log.v(Fruit.TAG, "> onSharedPreferenceChanged()");
		}
	}
	
	/**
	 * Return the number of points the current fruit is worth and hide from the screen.
	 * 
	 * @return Integer point value.
	 */
	public int eat() {
		this.hide();
		return this.mType.points;
	}
	
	/**
	 * Get the current Fruit's type.
	 * 
	 * @return Fruit.Type
	 */
	public Fruit.Type getType() {
		return this.mType;
	}
	
	/**
	 * Move off screen and make invisible.
	 */
	private void hide() {
		if (this.mIsTrophyEdenEnabled) {
			//Randomize next fruit
			this.mType = Fruit.Type.values()[Game.RANDOM.nextInt(Fruit.Type.values().length)];
			
			if (this.mPositions != null) {
				//Show it!
				this.show();
			}
		} else {
			this.mIsVisible = false;
			this.mPosition.set(-1, -1);
		}
	}
	
	/**
	 * Whether or not the fruit is on the board.
	 * 
	 * @return Boolean.
	 */
	public boolean isVisible() {
		return this.mIsVisible;
	}
	
    @Override
	public void performResize(final Game game) {
    	super.performResize(game);
    	
    	final int dotCols = game.getIconCols() + 1;
    	final int dotRows = game.getIconRows() + 1;
    	final int dotsCol = game.getCellColumnSpacing() + 1;
    	final int dotsRow = game.getCellRowSpacing() + 1;
    	
    	//Get all possible fruit positions
    	this.mPositions.clear();
    	for (int i = 0; i < dotCols; i++) {
    		for (int j = 0; j < dotRows; j++) {
    			final Point position = new Point(i * dotsCol, j * dotsRow);
    			if (game.isValidBoardPosition(position)) {
    				this.mPositions.add(position);
    			}
    		}
    	}
    	
    	if (this.mIsTrophyEdenEnabled) {
    		this.show();
    	}
    }
	
	@Override
	public void tick(final Game game) {
		if (this.mIsVisible) {
			if ((System.currentTimeMillis() - this.mCreated) > this.mVisibleLength) {
				this.hide();
			}
		} else {
			final int dotsEaten = game.getDotsEaten();
			if (((dotsEaten > this.mThresholdFirst) && (this.mNumberDisplayed == 0)) || ((dotsEaten > this.mThresholdSecond) && (this.mNumberDisplayed == 1))) {
				this.show();
			}
		}
	}

	/**
	 * Show fruit at random position
	 */
	private void show() {
	    this.mIsVisible = true;
	    this.mNumberDisplayed += 1;
	    this.mVisibleLength = Game.RANDOM.nextInt(this.mVisibleUpper - this.mVisibleLower + 1) + this.mVisibleLower;
	    this.setPosition(this.mPositions.get(Game.RANDOM.nextInt(this.mPositions.size())));
	    this.mCreated = System.currentTimeMillis();
    }

	@Override
	public void draw(final Game game, final Canvas c) {
		if (this.mIsVisible) {
			c.save();
			c.translate(this.mLocation.x - this.mCellWidthOverTwo, this.mLocation.y - this.mCellHeightOverTwo);
			
			switch (this.mCharacter) {
				case FRUIT:
					//two to four daily servings...
					c.drawBitmap(this.mFruits, this.mType.sprite, game.getCellSize(), null);
					break;
					
				case GOOGOL:
			    	c.setDrawFilter(Game.FILTER_SET);
					c.drawBitmap(this.mFruits, null, game.getCellSize(), Entity.SPRITE_PAINT);
			    	c.setDrawFilter(Game.FILTER_REMOVE);
					break;
			}
			
			c.restore();
		}
	}

	@Override
	protected void moved(final Game game) {
		//We do not move
	}

	@Override
	protected void newLevel(final Game game) {
		this.hide();
		this.mNumberDisplayed = 0;
		this.mType = Fruit.getForLevel(game.getLevel());
	}
	
	@Override
	public void newLife(final Game game) {
		//We do not move
	}
	
	

	/**
	 * Return which type of fruit should appear on which level.
	 * 
	 * @param level The level you wish to get fruit for.
	 * @return The Type of fruit for the level.
	 */
	private static Fruit.Type getForLevel(final int level) {
		if (level <= 0) {
			throw new IllegalArgumentException("Level number must be greater than zero.");
		}
		
		switch (level) {
			case 1:
				return Fruit.Type.CHERRY;
			case 2:
				return Fruit.Type.STRAWBERRY;
			case 3:
			case 4:
				return Fruit.Type.PEACH;
			case 5:
			case 6:
				return Fruit.Type.APPLE;
			case 7:
			case 8:
				return Fruit.Type.GRAPES;
			case 9:
			case 10:
				return Fruit.Type.GALAXIAN;
			case 11:
			case 12:
				return Fruit.Type.BELL;
			default:
				return Fruit.Type.KEY;
		}
	}
}
