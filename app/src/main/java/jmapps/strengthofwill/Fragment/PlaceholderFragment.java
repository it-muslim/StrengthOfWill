package jmapps.strengthofwill.Fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Objects;

import jmapps.strengthofwill.DBSetup.DBAssetHelper;
import jmapps.strengthofwill.R;
import jmapps.strengthofwill.TypeFace.TypeFaces;

public class PlaceholderFragment extends Fragment implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private int sectionNumber;

    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    private SQLiteDatabase sqLiteDatabase;

    private NestedScrollView nvScrollMainContent;

    private TextView tvChapterName;
    private TextView tvChapterContent;

    private String strParagraphNumber;
    private String strChapterName;
    private String strChapterContent;

    private String strFootnoteId;
    private String strFootnoteContent;

    private PagesProgress pagesProgress;

    public static PlaceholderFragment newInstance(int sectionNumber) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        setRetainInstance(true);

        if (getArguments() != null) {
            sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mEditor = mPreferences.edit();

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        DBAssetHelper dbAssetHelper = new DBAssetHelper(getActivity());
        sqLiteDatabase = dbAssetHelper.getReadableDatabase();

        TextView tvParagraphNumber = rootView.findViewById(R.id.tv_paragraph_number);
        tvChapterName = rootView.findViewById(R.id.tv_chapter_name);
        tvChapterContent = rootView.findViewById(R.id.tv_chapter_content);

        ToggleButton tbAddRemoveBookmark = rootView.findViewById(R.id.tb_add_remove_bookmark);
        Button btnCopyContent = rootView.findViewById(R.id.btn_copy_content);

        boolean bookmarkState = mPreferences.getBoolean(
                "key_bookmark_chapter " + sectionNumber, false);
        tbAddRemoveBookmark.setChecked(bookmarkState);

        try {
            @SuppressLint("Recycle")
            Cursor cursor = sqLiteDatabase.query("TABLE_CHAPTERS",
                    new String[]{"Paragraph", "ChapterName", "ChapterContent"},
                    "_id = ?",
                    new String[]{String.valueOf(sectionNumber)},
                    null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {

                strParagraphNumber = cursor.getString(0);
                strChapterName = cursor.getString(1);
                strChapterContent = cursor.getString(2);

                tvChapterName.setMovementMethod(LinkMovementMethod.getInstance());
                tvChapterContent.setMovementMethod(LinkMovementMethod.getInstance());

                tvParagraphNumber.setText(Html.fromHtml(strParagraphNumber));

                tvChapterName.setText(stringBuilder(strChapterName), TextView.BufferType.SPANNABLE);
                tvChapterContent.setText(stringBuilder(strChapterContent), TextView.BufferType.SPANNABLE);
            }

        } catch (Exception e) {
            Toast.makeText(getActivity(), "База данных недоступна", Toast.LENGTH_SHORT).show();
        }

        setTextIndentAndSize();
        setTextFont();
        tbAddRemoveBookmark.setOnCheckedChangeListener(this);
        btnCopyContent.setOnClickListener(this);
        pagesProgress.setPageProgress(sectionNumber);

        nvScrollMainContent = rootView.findViewById(R.id.nv_scroll_main_content);
        nvScrollMainContent.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView nestedScrollView, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                mEditor.putInt(sectionNumber + " scrollX", scrollX);
                mEditor.putInt(sectionNumber + " scrollY", scrollY);
            }
        });

        loadLastNestedScrollState();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            pagesProgress = (PagesProgress) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " implement method");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_copy_content:
                copyContent();
                break;
        }
    }

    public interface PagesProgress {
        void setPageProgress(int sectionNumber);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ContentValues bookmark = new ContentValues();
        bookmark.put("Favorite", isChecked);

        if (isChecked) {
            Toast.makeText(getActivity(), "Добавлено в избранное", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Удалено из избранного", Toast.LENGTH_SHORT).show();
        }

        mEditor.putBoolean("key_bookmark_chapter " + sectionNumber, isChecked).apply();

        try {
            sqLiteDatabase.update("TABLE_CHAPTERS",
                    bookmark,
                    "_id = ?",
                    new String[]{String.valueOf(sectionNumber)});
        } catch (Exception e) {

            Toast.makeText(getActivity(), "База данных недоступна", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setTextIndentAndSize();
        setTextFont();
        loadLastNestedScrollState();
    }

    @Override
    public void onStop() {
        super.onStop();
        mEditor.apply();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sqLiteDatabase.close();
        mEditor.apply();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setTextIndentAndSize() {
        int valueIndent = mPreferences.getInt("key_indent_size", 16);
        int valueSize = mPreferences.getInt("key_text_size", 18);

        tvChapterContent.setPadding(valueIndent, 16, valueIndent, 16);
        tvChapterName.setTextSize(valueSize);
        tvChapterContent.setTextSize(valueSize);
    }

    private void setTextFont() {
        boolean fontOneState = mPreferences.getBoolean("key_font_one", true);
        boolean fontTwoState = mPreferences.getBoolean("key_font_two", false);
        boolean fontThreeState = mPreferences.getBoolean("key_font_three", false);

        if (fontOneState) {

            tvChapterName.setTypeface(TypeFaces.get(getContext(), "fonts/veranda.ttf"));
            tvChapterContent.setTypeface(TypeFaces.get(getContext(), "fonts/veranda.ttf"));

        } else if (fontTwoState) {

            tvChapterName.setTypeface(TypeFaces.get(getContext(), "fonts/arial.ttf"));
            tvChapterContent.setTypeface(TypeFaces.get(getContext(), "fonts/arial.ttf"));

        } else if (fontThreeState) {

            tvChapterName.setTypeface(TypeFaces.get(getContext(), "fonts/times.ttf"));
            tvChapterContent.setTypeface(TypeFaces.get(getContext(), "fonts/times.ttf"));
        }
    }

    private SpannableStringBuilder stringBuilder(String str) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(Html.fromHtml(str));

        str = ssb.toString();

        int indexOne = str.indexOf("[");
        final int[] indexTwo = {0};

        while (indexOne != -1) {
            indexTwo[0] = str.indexOf("]", indexOne) + 1;

            String clickString = str.substring(indexOne, indexTwo[0]);
            clickString = clickString.substring(1, clickString.length() - 1);
            final String finalClickString = clickString;

            ssb.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {

                    try {
                        @SuppressLint("Recycle")
                        Cursor cursor = sqLiteDatabase.query("TABLE_FOOTNOTE",
                                new String[]{"_id", "Footnote"},
                                "_id = ?",
                                new String[]{finalClickString},
                                null, null, null);

                        if ((cursor != null) && cursor.moveToFirst()) {

                            strFootnoteId = cursor.getString(0);
                            strFootnoteContent = cursor.getString(1);
                        }

                        if (cursor != null && !cursor.isClosed()) {
                            cursor.close();
                        }

                    } catch (Exception e) {

                        Toast.makeText(getContext(), "База данных недоступна", Toast.LENGTH_SHORT).show();
                    }

                    DialogFootnote(strFootnoteId, strFootnoteContent);

                }
            }, indexOne, indexTwo[0], 0);
            indexOne = str.indexOf("[", indexTwo[0]);
        }
        return ssb;
    }

    @SuppressLint("SetTextI18n")
    private void DialogFootnote(String strFootnoteId, String strFootnoteContent) {
        @SuppressLint("InflateParams")
        View footnoteView = getLayoutInflater().inflate(R.layout.dialog_footnote, null);
        AlertDialog.Builder footnoteDialog = new AlertDialog.Builder(getActivity());
        footnoteDialog.setView(footnoteView);
        TextView footnoteIdNumber = footnoteView.findViewById(R.id.tv_footnote_id_number);
        TextView footnoteContent = footnoteView.findViewById(R.id.tv_footnote_content);
        footnoteIdNumber.setText("Сноска [" + strFootnoteId + "]");
        footnoteContent.setText(Html.fromHtml(strFootnoteContent));
        footnoteDialog.create().show();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void copyContent() {
        ClipboardManager clipboardManager = (ClipboardManager)
                Objects.requireNonNull(getContext()).getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData copyData = ClipData.newPlainText("",
                Html.fromHtml(strParagraphNumber + "<p/>" + strChapterName + "<p/>" +
                        strChapterContent + "<p/>" +
                        "_____________________" + "<p/>" +
                        "https://play.google.com/store/apps/details?id=jmapps.strengthofwill"));
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(copyData);
            Toast.makeText(getActivity(), "Скопировано в буфер", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLastNestedScrollState() {
        final int scrollX = mPreferences.getInt(sectionNumber + " scrollX", 0);
        final int scrollY = mPreferences.getInt(sectionNumber + " scrollY", 0);

        nvScrollMainContent.post(new Runnable() {
            @Override
            public void run() {
                nvScrollMainContent.scrollTo(scrollX, scrollY);
            }
        });
    }
}