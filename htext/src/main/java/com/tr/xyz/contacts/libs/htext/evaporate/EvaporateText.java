package com.tr.xyz.contacts.libs.htext.evaporate;


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.tr.xyz.contacts.libs.htext.base.CharacterDiffResult;
import com.tr.xyz.contacts.libs.htext.base.CharacterUtils;
import com.tr.xyz.contacts.libs.htext.base.DefaultAnimatorListener;
import com.tr.xyz.contacts.libs.htext.base.HText;
import com.tr.xyz.contacts.libs.htext.base.HTextView;

import java.util.ArrayList;
import java.util.List;


/**
 * EvaporateText
 * Created by hanks on 2017/3/16.
 */
public class EvaporateText extends HText {
   private final List<CharacterDiffResult> differentList = new ArrayList<>();
   float charTime  = 300;
   int   mostCount = 20;
   private int           mTextHeight;
   private long          duration;
   private ValueAnimator animator;

   @Override
   public void init(final HTextView hTextView, AttributeSet attrs, int defStyle) {

      super.init(hTextView, attrs, defStyle);
      animator = new ValueAnimator();
      animator.setInterpolator(new AccelerateDecelerateInterpolator());
      animator.addListener(new DefaultAnimatorListener() {
         @Override
         public void onAnimationEnd(Animator animation) {
            if (animationListener != null) {
               animationListener.onAnimationEnd(mHTextView);
            }
         }
      });
      animator.addUpdateListener(animation -> {
         progress = (float) animation.getAnimatedValue();
         mHTextView.invalidate();
      });
      int n = mText.length();
      n        = n <= 0 ? 1 : n;
      duration = (long) (charTime + charTime / mostCount * (n - 1));
   }

   @Override
   public void animateText(final CharSequence text) {
      mHTextView.post(new Runnable() {
         @Override
         public void run() {

            if (mHTextView.getLayout() != null) {
               oldStartX = mHTextView.getLayout().getLineLeft(0);
            }


            EvaporateText.super.animateText(text);
         }
      });
   }

   @Override
   protected void initVariables() {}

   @Override
   protected void animateStart(CharSequence text) {
      if (mText == null || mOldText == null) return;
      int n = mText.length();
      n        = n <= 0 ? 1 : n;
      duration = (long) (charTime + charTime / mostCount * (n - 1));
      animator.cancel();
      animator.setFloatValues(0, 1);
      animator.setDuration(duration);
      animator.start();
   }

   @Override
   protected void animatePrepare(CharSequence text) {
      if (mText == null || mOldText == null) return;
      differentList.clear();
      differentList.addAll(CharacterUtils.diff(mOldText, mText));

      Rect bounds = new Rect();
      mPaint.getTextBounds(mText.toString(), 0, mText.length(), bounds);
      mTextHeight = bounds.height();
   }

   @Override
   protected void drawFrame(Canvas canvas) {
      if (mText == null || mOldText == null) return;
      if (mHTextView.getLayout() == null) return;

      float startX = mHTextView.getLayout().getLineLeft(0);
      float startY = mHTextView.getBaseline();

      float offset    = startX;
      float oldOffset = oldStartX;

      int maxLength = Math.max(mText.length(), mOldText.length());

      for (int i = 0; i < maxLength; i++) {

         // draw old text
         if (i < mOldText.length()) {
            //
            float pp = progress * duration / (charTime + charTime / mostCount * (mText.length() - 1));

            mOldPaint.setTextSize(mTextSize);

            int move = CharacterUtils.needMove(i, differentList);

            if (move != -1) {
               mOldPaint.setAlpha(255);
               float p = pp * 2f;
               p = p > 1 ? 1 : p;
               float distX = CharacterUtils.getOffset(i, move, p, startX, oldStartX, gapList, oldGapList);
               canvas.drawText(mOldText.charAt(i) + "", 0, 1, distX, startY, mOldPaint);
            }
            else {
               mOldPaint.setAlpha((int) ((1 - pp) * 255));
               float y     = startY - pp * mTextHeight;
               float width = mOldPaint.measureText(mOldText.charAt(i) + "");
               canvas.drawText(mOldText.charAt(i) + "", 0, 1, oldOffset + (oldGapList.get(i) - width) / 2, y, mOldPaint);
            }
            oldOffset += oldGapList.get(i);
         }

         // draw new text
         if (i < mText.length()) {

            if (!CharacterUtils.stayHere(i, differentList)) {

               int alpha = (int) (255f / charTime * (progress * duration - charTime * i / mostCount));
               alpha = Math.min(alpha, 255);
               alpha = Math.max(alpha, 0);

               mPaint.setAlpha(alpha);
               mPaint.setTextSize(mTextSize);
               float pp = progress * duration / (charTime + charTime / mostCount * (mText.length() - 1));
               float y  = mTextHeight + startY - pp * mTextHeight;

               float width = mPaint.measureText(mText.charAt(i) + "");
               canvas.drawText(mText.charAt(i) + "", 0, 1, offset + (gapList.get(i) - width) / 2, y, mPaint);
            }

            offset += gapList.get(i);
         }
      }
   }

}
