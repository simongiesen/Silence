package org.smssecure.smssecure;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class BasicIntroFragment extends Fragment {

  private static final String TAG = BasicIntroFragment.class.getSimpleName();

  private static final String ARG_DRAWABLE  = "drawable";
  private static final String ARG_TEXT      = "text";
  private static final String ARG_SUBTEXT   = "subtext";
  private static final String ARG_LINK_TEXT = "link_text";
  private static final String ARG_ACTIVITY  = "activity";

  private int    drawable;
  private int    text;
  private int    subtext;
  private int    linkText;
  private String activity;

  public static BasicIntroFragment newInstance(int drawable, int text, int subtext, int linkText, String activity) {
    BasicIntroFragment fragment = new BasicIntroFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_DRAWABLE, drawable);
    args.putInt(ARG_TEXT, text);
    args.putInt(ARG_SUBTEXT, subtext);
    args.putInt(ARG_LINK_TEXT, linkText);

    args.putString(ARG_ACTIVITY, activity);

    fragment.setArguments(args);
    return fragment;
  }

  public BasicIntroFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      drawable = getArguments().getInt(ARG_DRAWABLE );
      text     = getArguments().getInt(ARG_TEXT     );
      subtext  = getArguments().getInt(ARG_SUBTEXT  );
      linkText = getArguments().getInt(ARG_LINK_TEXT);

      activity = getArguments().getString(ARG_ACTIVITY);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.color_fragment, container, false);

    ((ImageView)v.findViewById(R.id.watermark)).setImageResource(drawable);
    ((TextView)v.findViewById(R.id.blurb)).setText(text);

    if (linkText != -1) {
      TextView subBlurbView = (TextView) v.findViewById(R.id.subblurb);
      SpannableString spannableString = new SpannableString(getString(subtext) + " " + getString(linkText));
      ClickableSpan clickableSpan = new ClickableSpan() {
        @Override
        public void onClick(View widget) {
          if (activity != null) {
            try {
              Intent intent = new Intent(getActivity(), Class.forName(activity));
              getActivity().startActivity(intent);
            } catch (ClassNotFoundException e) {
              Log.w(TAG, e);
            }
          }
        }

        @Override
        public void updateDrawState(TextPaint textPaint) {
          textPaint.linkColor = getResources().getColor(org.smssecure.smssecure.R.color.white);
          super.updateDrawState(textPaint);
        }
      };
      spannableString.setSpan(clickableSpan, getString(subtext).length() + 1, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      subBlurbView.setMovementMethod(LinkMovementMethod.getInstance());
      subBlurbView.setText(spannableString);
    } else {
      ((TextView)v.findViewById(R.id.subblurb)).setText(subtext);
    }

    return v;
  }
}
