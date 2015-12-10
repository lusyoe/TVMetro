package com.example.library;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;


/**
 * 功能简介：获取焦点时放大
 */
public class TVMetroRelativeLayout extends RelativeLayout {
	/**
	 * 无效果
	 */
	public final static int ANIM_DEFAULT = 0;
	
	/**
	 * 平移
	 */
	public final static int ANIM_TRASLATE = 1;
	
	/**
	 * 光标tag
	 */
	private final String cursorTag = "cursor";
	
	/**
	 * 光标
	 */
	private View cursor;
	
	/**
	 * 光标资源
	 */
	private int cursorRes; 
	
	/**
	 * 光标飘移动画 默认无效果
	 */
	private int animationType;
	
	/**
	 * 放大用时
	 */
	private int durationLarge = 100;
	/**
	 * 缩小用时
	 */
	private int durationSmall = 100;
	/**
	 * 滑动用时
	 */
	private int durationTranslate; 
	 
	/**
	 * 放大高度
	 */
	private int biggerHeight = 40;
	
	/**
	 * 放大宽度
	 */
	private int biggerWidth = 40;
	
	/**
	 * 阴影宽高
	 */
	private int shadow = 5; 
	
	/**
	 * 光标高度
	 */
	private int cursorHeight = 0;
	
	/**
	 * 光标宽度
	 */
	private int cursorWidth = 0;
	
	/**
	 * 鼠标二次点击
	 */
	private boolean clickSecond = true;
	
	private AnimatorSet animatorSet;
	
	private ObjectAnimator largeX;

	private OnChildSelectListener onChildSelectListener;
	
	private OnChildClickListener onChildClickListener;

	private View focusView = null;
	
	public void setBiggerHeight(int biggerHeight){
		this.biggerHeight = biggerHeight;
	}
	
	public void setBiggerWidth(int biggerWidth){
		this.biggerWidth = biggerWidth;
	}
	
	public void setShadow(int shadow){
		this.shadow = shadow;
	}
	
	public int getCursorRes() {
		return cursorRes;
	}

	public void setCursorRes(int cursorRes) {
		this.cursorRes = cursorRes;
	} 

	public TVMetroRelativeLayout(Context context) {
		this(context, null);
	}

	public TVMetroRelativeLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TVMetroRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray custom = getContext().obtainStyledAttributes(attrs,R.styleable.TVMetroRelativeLayout);
		
		this.cursorRes = custom.getResourceId(R.styleable.TVMetroRelativeLayout_cursorRes, 0);
		this.animationType = custom.getInt(R.styleable.TVMetroRelativeLayout_animationType, 0);
		this.durationLarge = custom.getInteger(R.styleable.TVMetroRelativeLayout_durationLarge, 100);
		this.durationSmall = custom.getInteger(R.styleable.TVMetroRelativeLayout_durationSmall, 100);
		this.durationTranslate = custom.getInteger(R.styleable.TVMetroRelativeLayout_durationTranslate, 9);
		this.biggerHeight = (int) custom.getDimension(R.styleable.TVMetroRelativeLayout_biggerHeight, 40);
		this.biggerWidth = (int) custom.getDimension(R.styleable.TVMetroRelativeLayout_biggerWidth, 40);
		this.shadow = (int) custom.getDimension(R.styleable.TVMetroRelativeLayout_shadow, 10);
		this.clickSecond = custom.getBoolean(R.styleable.TVMetroRelativeLayout_clickSecond, true);
		custom.recycle();
		// 关闭子控件动画缓存 使嵌套动画更流畅
		setAnimationCacheEnabled(false);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (changed) {
			super.onLayout(changed, l, t, r, b);
		} else {
			int cCount = getChildCount();
			int cWidth = 0;
			int cHeight = 0;
			/**
			 * 遍历所有childView根据其宽和高，以及margin进行布局 选中框无需重定位??初始位置
			 */
			for (int i = 0; i < cCount; i++) {
				View childView = getChildAt(i);

				if (childView.getTag() != null && childView.getTag().toString().equals(cursorTag)) {
					continue;
				}

				cWidth = childView.getMeasuredWidth();
				cHeight = childView.getMeasuredHeight();

				int cl = 0, ct = 0, cr = 0, cb = 0;

				cl = childView.getLeft();
				ct = childView.getTop();
				cr = cl + cWidth;
				cb = cHeight + ct;
				childView.layout(cl, ct, cr, cb);
			}
		}

	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		bindEvent();
	}

	// 初始化焦点
	private void bindEvent() {
		bindOnChildFocusEvent();
		
		// 初始化焦点
		final View focus = findFocus();
		if (focus != null) {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					focus.requestFocus();
					moveCover(focus);
				}
			}, 300);
		}
	}

	/**
	 * 光标移动 到达后 与控件同时放大
	 */
	private void moveCover(View item) {
		if (cursor == null) {
			cursor = new View(getContext());
			cursor.setTag(cursorTag);
			cursor.setBackgroundResource(cursorRes);
			//给一个初始大小
			LayoutParams params = new LayoutParams(50, 50);
			cursor.setLayoutParams(params);
			this.addView(cursor);
		}
		setBorderParams(item);
	}

	/**
	 * 还原控件状态
	 */

	private void returnCover(View item) {
		if (cursor == null) {
			return;
		}
		cursor.setVisibility(View.INVISIBLE);
		scaleToNormal(item);
	}

	private void scaleToLarge(View item) {
		if (!item.isFocused()) {
			return;
		}

		animatorSet = new AnimatorSet(); 
		largeX = ObjectAnimator.ofFloat(item, "ScaleX", 1f, biggerWidth * 1.0f / item.getMeasuredWidth() + 1);
		ObjectAnimator largeY = ObjectAnimator.ofFloat(item, "ScaleY", 1f, biggerHeight * 1.0f / item.getMeasuredHeight() + 1);
		
		//放大效果不从1开始的原因是：和放大的组件进行同步
		ObjectAnimator cursorX = ObjectAnimator.ofFloat(cursor, "ScaleX", (cursorWidth + shadow) * 1.0f / cursorWidth, (cursorWidth + biggerWidth + shadow) * 1.0f / cursorWidth);
		ObjectAnimator cursorY = ObjectAnimator.ofFloat(cursor, "ScaleY", (cursorHeight + shadow) * 1.0f / cursorHeight, (cursorHeight + biggerHeight + shadow) * 1.0f / cursorHeight);

		animatorSet.setDuration(durationLarge);
		animatorSet.play(largeX).with(largeY).with(cursorX).with(cursorY);
		animatorSet.start();
	}

	private void scaleToNormal(View item) {
		if (animatorSet == null) {
			return;
		}
		if (animatorSet.isRunning()) {
			animatorSet.cancel();
		}
		ObjectAnimator oa = ObjectAnimator.ofFloat(item, "ScaleX", 1f);
		oa.setDuration(durationSmall);
		oa.start();
		ObjectAnimator oa2 = ObjectAnimator.ofFloat(item, "ScaleY", 1f);
		oa2.setDuration(durationSmall);
		oa2.start();
	} 

	/**
	 * 指定光标相对位置
	 */
	private void setBorderParams(View item) {
		cursor.clearAnimation();
		cursor.setVisibility(View.VISIBLE);

		// 判断类型
		LayoutParams params = (LayoutParams) item.getLayoutParams();
		final int l = item.getLeft();
		final int t = item.getTop();
		final int r = item.getLeft() + params.width;
		final int b = item.getTop() + params.height;
		cursorWidth = r - l;
		cursorHeight = b - t;
		switch (animationType) {
			case ANIM_DEFAULT:
				cursor.layout(l, t, r, b);
				item.bringToFront();
				cursor.bringToFront();
				scaleToLarge(item);
				break;
			case ANIM_TRASLATE:
				if (cursor.getLeft() <= 0) {
					cursor.layout(l, t, r, b);
				}
				item.bringToFront();
				cursor.bringToFront();
	
				TvAnimator animator = new TvAnimator(cursor, item);
				animator.setTargetParams(l, t, r - l, b - t);
				animator.execute();
				break;
		}

	}

	class TvAnimator extends AsyncTask<Void, Integer, Integer> {
		private View target, item;
		private int l, t, cl, ct;
		private int width, cwidth, height, cheight;
		private int dX, dY, dW, dH;
		private int frequence = 17;  
		private int cbiggerWidth;
		private int cbiggerHeight;
		private int animDuration = 6;
		
		TvAnimator(View target, View item) {
			this.target = target;
			this.item = item;
		}

		public void setTargetParams(int l, int t, int width, int height) {
			this.l = l;
			this.t = t;
			this.width = width;
			this.height = height;
			this.cl = target.getLeft();
			this.ct = target.getTop();
			this.cwidth = target.getWidth();
			this.cheight = target.getHeight(); 
			this.cbiggerWidth = shadow + biggerWidth;
			this.cbiggerHeight = shadow + biggerHeight;
		} 
		
		@Override
		protected void onPreExecute() {
			dW = (int) (width - cwidth > 0 ? Math.ceil((width - cwidth)
					/ (frequence * 1.0)) : Math.floor((width - cwidth)
					/ (frequence * 1.0)));
			dH = (int) (height - cheight > 0 ? Math.ceil((height - cheight)
					/ (frequence * 1.0)) : Math.floor((height - cheight)
					/ (frequence * 1.0)));
			dX = (int) (l - cl > 0 ? Math.ceil((l - cl) / (frequence * 1.0))
					: Math.floor((l - cl) / (frequence * 1.0)));
			dY = (int) (t - ct >= 0 ? Math.ceil((t - ct) / (frequence * 1.0))
					: Math.floor((t - ct) / (frequence * 1.0)));
		}
 
		@Override
		protected Integer doInBackground(Void... params) {

			while (cl != l || t != ct || cwidth != width || cheight != height) {

				if (Math.abs(cl - l) <= Math.abs(dX)) {
					cl = l;
				} else {
					cl += dX;
				}

				if (Math.abs(ct - t) <= Math.abs(dY)) {
					ct = t;
				} else {
					ct += dY;
				}

				if (Math.abs(width - cwidth) <= Math.abs(dW)) {
					cwidth = width;
					dW = 0;
				} else {
					cwidth += dW;
				}

				if (Math.abs(height - cheight) <= Math.abs(dH)) {
					cheight = height;
					dH = 0;
				} else {
					cheight += dH;
				} 

				publishProgress(cl, ct, cl + cwidth + dW, ct + cheight + dH);
				try {
					Thread.sleep(durationTranslate);
				} catch (InterruptedException e) {
				}
			}

			return null;
		}

		/** 
         * 
         */
		@Override
		protected void onPostExecute(Integer integer) {
			if (!item.isFocused()) {
				return;
			}
			animatorSet = new AnimatorSet();
			ValueAnimator largeX = ObjectAnimator.ofFloat(item, "ScaleX", 1f, biggerWidth * 1.0f / width + 1);
			ValueAnimator largeY = ObjectAnimator.ofFloat(item, "ScaleY", 1f, biggerHeight * 1.0f / height + 1);

			animatorSet.setDuration(durationLarge);
			animatorSet.setInterpolator(new DecelerateInterpolator(3.0f));
			animatorSet.play(largeX).with(largeY);
			animatorSet.start();
			
			// 中心放大 左上减 右下加 
			ValueAnimator animation = ValueAnimator.ofFloat(1f, animDuration);
			animation.setDuration(durationLarge);
			animation.setInterpolator(new DecelerateInterpolator(3.0f));
			animation.addUpdateListener(new AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) { 
					float curScale = (Float) animation.getAnimatedValue(); 
					int dw = (int) (cbiggerWidth * curScale / (animDuration * 2 ));
					int dh = (int) (cbiggerHeight * curScale / (animDuration * 2 ));
					target.layout(l - dw, t - dh, l + cwidth + dw, t + cheight + dh);
				}
			});
			animation.start(); 
		}
 
		@Override
		protected void onProgressUpdate(Integer... values) {
			target.layout(values[0], values[1], values[2], values[3]);
		}
	}

	private void bindOnChildClickEvent(){
		if (getChildCount() < 1) {
			return;
		}
		View child = null;
		for (int i = 0; i < getChildCount(); i++) {
			child = getChildAt(i);
			if (child != null) {
				if (onChildClickListener != null) {
					child.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View child) {
							if(clickSecond){	//鼠标二次点击才能正在激发控件的点击事件
								if(child == focusView){
									onChildClickListener.onChildClick(child);
								}else{ 
									child.setFocusable(true);
									child.setFocusableInTouchMode(true);
									child.requestFocus();
									child.requestFocusFromTouch();
								}
							}else{
								onChildClickListener.onChildClick(child);
							}
						}
					});
				}
			}
		}

	}
	
	private void bindOnChildSelectEvent(){
		if (getChildCount() < 1) {
			return;
		}
		View child = null;
		for (int i = 0; i < getChildCount(); i++) {
			child = getChildAt(i);
			if (child != null) {
				child.setOnFocusChangeListener(new OnFocusChangeListener() {
					@Override
					public void onFocusChange(final View child, boolean focus) {
						if (focus) {
							focusView = child;
							new Handler().postDelayed(new Runnable() {
								@Override
								public void run() {
									moveCover(child);
								}
							}, 0);
							// 选中事件
							if (onChildSelectListener != null) {
								onChildSelectListener.onChildSelect(child);
							}
						} else {
							returnCover(child);
						}
					}
				});
			}
		}
	} 
	
	private void bindOnChildFocusEvent(){
		if (getChildCount() < 1) {
			return;
		}
		View child = null;
		for (int i = 0; i < getChildCount(); i++) {
			child = getChildAt(i);
			if (child != null) {
				child.setOnFocusChangeListener(new OnFocusChangeListener() {
					@Override
					public void onFocusChange(final View child, boolean focus) {
						if (focus) {
							focusView = child;
							new Handler().postDelayed(new Runnable() {
								@Override
								public void run() {
									moveCover(child);
								}
							}, 0); 
						} else {
							returnCover(child);
						}
					}
				});
			}
		}
	}
	
	public void setOnChildSelectListener(OnChildSelectListener onChildSelectListener) {
		this.onChildSelectListener = onChildSelectListener;
		bindOnChildSelectEvent();
	}

	public void setOnChildClickListener(OnChildClickListener onChildClickListener) {
		this.onChildClickListener = onChildClickListener;
		bindOnChildClickEvent();
	}

	public interface OnChildSelectListener {
		public void onChildSelect(View child);
	}

	public interface OnChildClickListener {
		public void onChildClick(View child);
	}
}
