package com.rahomi.lineup;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "RahomiLineupPrefs";
    private static final int PICK_IMAGE_REQUEST = 2026;

    private TextView tvAppTitle;
    private EditText etTeamName;
    private Spinner spPlayerCount;
    private Button btnSavePng, btnReset;
    private FrameLayout pitchContainer;
    private LinearLayout rosterContainer;

    private PitchOverlayView pitchLinesView;
    private final List<View> playerNodes = new ArrayList<>();

    private String teamName = "تشكيلة رحومي";
    private int playerCount = 11;
    private int activePhotoPlayerIndex = -1;

    private final List<String> playerNames = new ArrayList<>();
    private final List<String> playerNumbers = new ArrayList<>();
    private final List<String> playerPhotosBase64 = new ArrayList<>();
    private final List<float[]> playerPositions = new ArrayList<>();

    private static final float[][] PRESET_5 = {
            {0.50f, 0.86f}, {0.25f, 0.64f}, {0.75f, 0.64f}, {0.35f, 0.34f}, {0.65f, 0.34f}
    };
    private static final float[][] PRESET_6 = {
            {0.50f, 0.87f}, {0.24f, 0.67f}, {0.76f, 0.67f}, {0.50f, 0.48f}, {0.28f, 0.26f}, {0.72f, 0.26f}
    };
    private static final float[][] PRESET_7 = {
            {0.50f, 0.88f}, {0.22f, 0.68f}, {0.50f, 0.68f}, {0.78f, 0.68f}, {0.28f, 0.42f}, {0.72f, 0.42f}, {0.50f, 0.21f}
    };
    private static final float[][] PRESET_8 = {
            {0.50f, 0.88f}, {0.20f, 0.70f}, {0.40f, 0.70f}, {0.60f, 0.70f}, {0.80f, 0.70f}, {0.30f, 0.43f}, {0.70f, 0.43f}, {0.50f, 0.20f}
    };
    private static final float[][] PRESET_9 = {
            {0.50f, 0.89f}, {0.20f, 0.71f}, {0.40f, 0.71f}, {0.60f, 0.71f}, {0.80f, 0.71f}, {0.26f, 0.45f}, {0.50f, 0.45f}, {0.74f, 0.45f}, {0.50f, 0.20f}
    };
    private static final float[][] PRESET_10 = {
            {0.50f, 0.89f}, {0.18f, 0.72f}, {0.39f, 0.72f}, {0.61f, 0.72f}, {0.82f, 0.72f}, {0.24f, 0.47f}, {0.50f, 0.47f}, {0.76f, 0.47f}, {0.34f, 0.21f}, {0.66f, 0.21f}
    };
    private static final float[][] PRESET_11 = {
            {0.50f, 0.90f}, {0.18f, 0.73f}, {0.39f, 0.73f}, {0.61f, 0.73f}, {0.82f, 0.73f}, {0.22f, 0.49f}, {0.50f, 0.49f}, {0.78f, 0.49f}, {0.22f, 0.23f}, {0.50f, 0.18f}, {0.78f, 0.23f}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initDefaultSquad();
        loadFromPrefs();
        setContentView(buildUiLayout());

        pitchLinesView = new PitchOverlayView(this);
        pitchContainer.addView(pitchLinesView, 0, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setupSpinner();
        setupInputsAndButtons();
        rebuildPitchAndRoster();

        pitchContainer.post(this::positionAllNodes);
    }

    private View buildUiLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.setBackgroundColor(Color.parseColor("#080C10"));
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));

        tvAppTitle = new TextView(this);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.bottomMargin = dp(10);
        tvAppTitle.setLayoutParams(titleLp);
        tvAppTitle.setText("تشكيلة رحومي");
        tvAppTitle.setTextColor(Color.parseColor("#00FF66"));
        tvAppTitle.setTextSize(22f);
        tvAppTitle.setGravity(Gravity.CENTER);
        tvAppTitle.setTypeface(Typeface.DEFAULT_BOLD);

        LinearLayout topRow = new LinearLayout(this);
        LinearLayout.LayoutParams topRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topRowLp.bottomMargin = dp(8);
        topRow.setLayoutParams(topRowLp);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        etTeamName = new EditText(this);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        etTeamName.setLayoutParams(etLp);
        etTeamName.setHint("اسم الفريق...");
        etTeamName.setText("تشكيلة رحومي");
        etTeamName.setTextColor(Color.parseColor("#F3F7FA"));
        etTeamName.setHintTextColor(Color.parseColor("#8FA1B3"));
        etTeamName.setBackgroundColor(Color.parseColor("#16202C"));
        etTeamName.setPadding(dp(12), 0, dp(12), 0);
        etTeamName.setSingleLine(true);

        spPlayerCount = new Spinner(this);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(44));
        spLp.setMarginStart(dp(8));
        spPlayerCount.setLayoutParams(spLp);
        spPlayerCount.setBackgroundColor(Color.parseColor("#16202C"));
        spPlayerCount.setPadding(dp(8), 0, dp(8), 0);

        topRow.addView(etTeamName);
        topRow.addView(spPlayerCount);

        LinearLayout actionRow = new LinearLayout(this);
        LinearLayout.LayoutParams actionRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionRowLp.bottomMargin = dp(12);
        actionRow.setLayoutParams(actionRowLp);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        btnSavePng = new Button(this);
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        btnSavePng.setLayoutParams(saveLp);
        btnSavePng.setText("حفظ صورة PNG");
        btnSavePng.setTextColor(Color.parseColor("#06110A"));
        btnSavePng.setTypeface(Typeface.DEFAULT_BOLD);
        btnSavePng.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00FF66")));

        btnReset = new Button(this);
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(44));
        resetLp.setMarginStart(dp(8));
        btnReset.setLayoutParams(resetLp);
        btnReset.setText("إعادة ضبط");
        btnReset.setTextColor(Color.parseColor("#F3F7FA"));
        btnReset.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#16202C")));

        actionRow.addView(btnSavePng);
        actionRow.addView(btnReset);

        pitchContainer = new FrameLayout(this);
        LinearLayout.LayoutParams pitchLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(460));
        pitchLp.bottomMargin = dp(14);
        pitchContainer.setLayoutParams(pitchLp);
        pitchContainer.setBackgroundColor(Color.parseColor("#114B26"));

        TextView tvRosterHeader = new TextView(this);
        LinearLayout.LayoutParams hdrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hdrLp.bottomMargin = dp(8);
        tvRosterHeader.setLayoutParams(hdrLp);
        tvRosterHeader.setText("قائمة اللاعبين والأرقام والصور (اضغط على الصورة لاختيارها من الجهاز)");
        tvRosterHeader.setTextColor(Color.parseColor("#F3F7FA"));
        tvRosterHeader.setTextSize(15f);
        tvRosterHeader.setTypeface(Typeface.DEFAULT_BOLD);

        rosterContainer = new LinearLayout(this);
        rosterContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rosterContainer.setOrientation(LinearLayout.VERTICAL);

        root.addView(tvAppTitle);
        root.addView(topRow);
        root.addView(actionRow);
        root.addView(pitchContainer);
        root.addView(tvRosterHeader);
        root.addView(rosterContainer);

        scrollView.addView(root);
        return scrollView;
    }

    private void initDefaultSquad() {
        playerNames.clear();
        playerNumbers.clear();
        playerPhotosBase64.clear();
        playerPositions.clear();

        for (int i = 0; i < 11; i++) {
            playerNames.add(i == 0 ? "الحارس" : "لاعب " + (i + 1));
            playerNumbers.add(String.valueOf(i + 1));
            playerPhotosBase64.add("");
            playerPositions.add(new float[]{PRESET_11[i][0], PRESET_11[i][1]});
        }
    }

    private float[][] getPresetForCount(int count) {
        switch (count) {
            case 5: return PRESET_5;
            case 6: return PRESET_6;
            case 7: return PRESET_7;
            case 8: return PRESET_8;
            case 9: return PRESET_9;
            case 10: return PRESET_10;
            default: return PRESET_11;
        }
    }

    private void applyPresetPositions(int count) {
        float[][] preset = getPresetForCount(count);
        playerPositions.clear();
        for (int i = 0; i < 11; i++) {
            if (i < preset.length) {
                playerPositions.add(new float[]{preset[i][0], preset[i][1]});
            } else {
                playerPositions.add(new float[]{0.5f, 0.5f});
            }
        }
    }

    private void setupSpinner() {
        String[] options = {
                "5 لاعبين (خماسي)",
                "6 لاعبين",
                "7 لاعبين (سباعي)",
                "8 لاعبين",
                "9 لاعبين",
                "10 لاعبين",
                "11 لاعب (كامل)"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPlayerCount.setAdapter(adapter);
        spPlayerCount.setSelection(Math.max(0, Math.min(6, playerCount - 5)), false);

        spPlayerCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selected = position + 5;
                if (selected != playerCount) {
                    playerCount = selected;
                    applyPresetPositions(playerCount);
                    saveToPrefs();
                    rebuildPitchAndRoster();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupInputsAndButtons() {
        etTeamName.setText(teamName);
        tvAppTitle.setText(teamName.trim().isEmpty() ? "تشكيلة رحومي" : teamName);

        etTeamName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                teamName = s.toString();
                tvAppTitle.setText(teamName.trim().isEmpty() ? "تشكيلة رحومي" : teamName);
                saveToPrefs();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        btnReset.setOnClickListener(v -> {
            teamName = "تشكيلة رحومي";
            playerCount = 11;
            etTeamName.setText(teamName);
            spPlayerCount.setSelection(6);
            initDefaultSquad();
            saveToPrefs();
            rebuildPitchAndRoster();
        });

        btnSavePng.setOnClickListener(v -> exportPitchAsPng());
    }

    private void rebuildPitchAndRoster() {
        for (View node : playerNodes) {
            pitchContainer.removeView(node);
        }
        playerNodes.clear();
        rosterContainer.removeAllViews();

        for (int i = 0; i < playerCount; i++) {
            View node = createPlayerNodeView(i);
            playerNodes.add(node);
            pitchContainer.addView(node);
            rosterContainer.addView(createRosterRow(i, node));
        }

        pitchContainer.post(this::positionAllNodes);
    }

    private View createPlayerNodeView(final int index) {
        LinearLayout node = new LinearLayout(this);
        node.setOrientation(LinearLayout.VERTICAL);
        node.setGravity(Gravity.CENTER);

        ImageView avatar = new ImageView(this);
        int size = dp(52);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setTag("avatar_" + index);

        Bitmap photo = decodeBase64(playerPhotosBase64.get(index));
        if (photo != null) {
            avatar.setImageBitmap(makeCircleBitmap(photo));
        } else {
            avatar.setImageBitmap(makeNumberCircleBitmap(playerNumbers.get(index)));
        }

        TextView label = new TextView(this);
        label.setTag("label_" + index);
        label.setText("#" + playerNumbers.get(index) + " " + playerNames.get(index));
        label.setTextColor(Color.WHITE);
        label.setTextSize(11f);
        label.setBackgroundColor(Color.parseColor("#E0080C10"));
        label.setPadding(dp(7), dp(2), dp(7), dp(2));

        node.addView(avatar);
        node.addView(label);

        node.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int w = pitchContainer.getWidth();
                int h = pitchContainer.getHeight();
                if (w <= 0 || h <= 0) return false;

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        pitchContainer.requestDisallowInterceptTouchEvent(true);
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;
                        float maxX = Math.max(0, w - v.getWidth());
                        float maxY = Math.max(0, h - v.getHeight());
                        newX = Math.max(0, Math.min(maxX, newX));
                        newY = Math.max(0, Math.min(maxY, newY));
                        v.setX(newX);
                        v.setY(newY);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        pitchContainer.requestDisallowInterceptTouchEvent(false);
                        float centerX = (v.getX() + v.getWidth() / 2f) / w;
                        float centerY = (v.getY() + v.getHeight() / 2f) / h;
                        playerPositions.set(index, new float[]{centerX, centerY});
                        saveToPrefs();
                        return true;
                }
                return false;
            }
        });

        return node;
    }

    private void positionAllNodes() {
        int w = pitchContainer.getWidth();
        int h = pitchContainer.getHeight();
        if (w <= 0 || h <= 0) return;

        for (int i = 0; i < playerNodes.size(); i++) {
            View node = playerNodes.get(i);
            node.measure(
                    View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST)
            );
            float[] pos = playerPositions.get(i);
            float targetX = (pos[0] * w) - (node.getMeasuredWidth() / 2f);
            float targetY = (pos[1] * h) - (node.getMeasuredHeight() / 2f);
            targetX = Math.max(0, Math.min(w - node.getMeasuredWidth(), targetX));
            targetY = Math.max(0, Math.min(h - node.getMeasuredHeight(), targetY));
            node.setX(targetX);
            node.setY(targetY);
        }
    }

    private View createRosterRow(final int index, final View nodeView) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(Color.parseColor("#16202C"));
        row.setPadding(dp(8), dp(8), dp(8), dp(8));

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowLp);

        final ImageView miniPic = new ImageView(this);
        int picSize = dp(44);
        miniPic.setLayoutParams(new LinearLayout.LayoutParams(picSize, picSize));
        miniPic.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap photo = decodeBase64(playerPhotosBase64.get(index));
        if (photo != null) {
            miniPic.setImageBitmap(makeCircleBitmap(photo));
        } else {
            miniPic.setImageBitmap(makeNumberCircleBitmap(playerNumbers.get(index)));
        }

        miniPic.setOnClickListener(v -> {
            activePhotoPlayerIndex = index;
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        final EditText etName = new EditText(this);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        nameLp.setMargins(dp(8), 0, dp(6), 0);
        etName.setLayoutParams(nameLp);
        etName.setText(playerNames.get(index));
        etName.setTextColor(Color.WHITE);
        etName.setBackgroundColor(Color.parseColor("#0E151D"));
        etName.setPadding(dp(8), 0, dp(8), 0);
        etName.setSingleLine(true);

        final EditText etNum = new EditText(this);
        LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(dp(54), dp(42));
        numLp.setMargins(0, 0, dp(6), 0);
        etNum.setLayoutParams(numLp);
        etNum.setText(playerNumbers.get(index));
        etNum.setTextColor(Color.parseColor("#00FF66"));
        etNum.setGravity(Gravity.CENTER);
        etNum.setBackgroundColor(Color.parseColor("#0E151D"));
        etNum.setSingleLine(true);

        Button btnClear = new Button(this);
        btnClear.setText("حذف صورة");
        btnClear.setTextSize(11f);
        btnClear.setTextColor(Color.WHITE);
        btnClear.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#253344")));

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                playerNames.set(index, etName.getText().toString());
                playerNumbers.set(index, etNum.getText().toString());
                TextView lbl = nodeView.findViewWithTag("label_" + index);
                if (lbl != null) {
                    lbl.setText("#" + playerNumbers.get(index) + " " + playerNames.get(index));
                }
                if (decodeBase64(playerPhotosBase64.get(index)) == null) {
                    ImageView av = nodeView.findViewWithTag("avatar_" + index);
                    if (av != null) {
                        av.setImageBitmap(makeNumberCircleBitmap(playerNumbers.get(index)));
                    }
                    miniPic.setImageBitmap(makeNumberCircleBitmap(playerNumbers.get(index)));
                }
                saveToPrefs();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };

        etName.addTextChangedListener(watcher);
        etNum.addTextChangedListener(watcher);

        btnClear.setOnClickListener(v -> {
            playerPhotosBase64.set(index, "");
            Bitmap numBmp = makeNumberCircleBitmap(playerNumbers.get(index));
            miniPic.setImageBitmap(numBmp);
            ImageView av = nodeView.findViewWithTag("avatar_" + index);
            if (av != null) {
                av.setImageBitmap(numBmp);
            }
            saveToPrefs();
        });

        row.addView(miniPic);
        row.addView(etName);
        row.addView(etNum);
        row.addView(btnClear);
        return row;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && activePhotoPlayerIndex >= 0) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    Bitmap raw = BitmapFactory.decodeStream(is);
                    if (is != null) is.close();
                    if (raw != null) {
                        Bitmap scaled = Bitmap.createScaledBitmap(raw, 220, 220, true);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos);
                        String b64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                        playerPhotosBase64.set(activePhotoPlayerIndex, b64);
                        saveToPrefs();
                        rebuildPitchAndRoster();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "تعذر تحميل الصورة", Toast.LENGTH_SHORT).show();
                }
            }
            activePhotoPlayerIndex = -1;
        }
    }

    private void exportPitchAsPng() {
        int outW = 900;
        int outH = 1200;
        Bitmap result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#114B26"));
        canvas.drawRect(0, 0, outW, outH, bgPaint);

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#88FFFFFF"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5f);
        canvas.drawRect(36, 36, outW - 36, outH - 36, linePaint);
        canvas.drawLine(36, outH / 2f, outW - 36, outH / 2f, linePaint);
        canvas.drawCircle(outW / 2f, outH / 2f, 120f, linePaint);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#00FF66"));
        titlePaint.setTextSize(42f);
        titlePaint.setFakeBoldText(true);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(teamName.trim().isEmpty() ? "تشكيلة رحومي" : teamName, outW / 2f, 95f, titlePaint);

        for (int i = 0; i < playerCount; i++) {
            float[] pos = playerPositions.get(i);
            float cx = pos[0] * outW;
            float cy = pos[1] * outH;
            float r = 46f;

            Bitmap photo = decodeBase64(playerPhotosBase64.get(i));
            Bitmap circle = (photo != null)
                    ? makeCircleBitmap(Bitmap.createScaledBitmap(photo, 140, 140, true))
                    : makeNumberCircleBitmap(playerNumbers.get(i));

            Bitmap scaledCircle = Bitmap.createScaledBitmap(circle, (int) (r * 2), (int) (r * 2), true);
            canvas.drawBitmap(scaledCircle, cx - r, cy - r, null);

            canvas.drawCircle(cx, cy, r, linePaint);

            Paint tagBg = new Paint();
            tagBg.setColor(Color.parseColor("#E5080C10"));
            canvas.drawRect(cx - 95, cy + r + 8, cx + 95, cy + r + 44, tagBg);

            Paint tagTxt = new Paint(Paint.ANTI_ALIAS_FLAG);
            tagTxt.setColor(Color.WHITE);
            tagTxt.setTextSize(22f);
            tagTxt.setFakeBoldText(true);
            tagTxt.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("#" + playerNumbers.get(i) + " " + playerNames.get(i), cx, cy + r + 33, tagTxt);
        }

        if (saveBitmapToGallery(result, "rahomi_lineup_" + System.currentTimeMillis() + ".png")) {
            Toast.makeText(this, "تم حفظ صورة التشكيلة بنجاح", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "حدث خطأ أثناء حفظ الصورة", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean saveBitmapToGallery(Bitmap bitmap, String fileName) {
        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RahomiLineup");
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return false;
                fos = getContentResolver().openOutputStream(uri);
            } else {
                String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                File folder = new File(dir, "RahomiLineup");
                if (!folder.exists()) folder.mkdirs();
                File file = new File(folder, fileName);
                fos = new FileOutputStream(file);
            }
            if (fos == null) return false;
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Bitmap decodeBase64(String b64) {
        if (b64 == null || b64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap makeCircleBitmap(Bitmap src) {
        int s = Math.min(src.getWidth(), src.getHeight());
        Bitmap squared = Bitmap.createBitmap(src, (src.getWidth() - s) / 2, (src.getHeight() - s) / 2, s, s);
        Bitmap out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Path path = new Path();
        path.addCircle(s / 2f, s / 2f, s / 2f, Path.Direction.CW);
        c.clipPath(path);
        c.drawBitmap(squared, 0, 0, p);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setColor(Color.parseColor("#00FF66"));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(s * 0.06f);
        c.drawCircle(s / 2f, s / 2f, (s / 2f) - (s * 0.03f), border);
        return out;
    }

    private Bitmap makeNumberCircleBitmap(String numText) {
        int s = 140;
        Bitmap out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(Color.parseColor("#16202C"));
        c.drawCircle(s / 2f, s / 2f, s / 2f - 4f, fill);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setColor(Color.parseColor("#00FF66"));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(6f);
        c.drawCircle(s / 2f, s / 2f, s / 2f - 4f, border);

        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.parseColor("#00FF66"));
        tp.setTextSize(46f);
        tp.setFakeBoldText(true);
        tp.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = tp.getFontMetrics();
        float y = (s / 2f) - (fm.ascent + fm.descent) / 2f;
        c.drawText(numText == null || numText.isEmpty() ? "1" : numText, s / 2f, y, tp);
        return out;
    }

    private void saveToPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString("teamName", teamName);
        ed.putInt("playerCount", playerCount);
        for (int i = 0; i < 11; i++) {
            ed.putString("name_" + i, playerNames.get(i));
            ed.putString("num_" + i, playerNumbers.get(i));
            ed.putString("photo_" + i, playerPhotosBase64.get(i));
            ed.putFloat("px_" + i, playerPositions.get(i)[0]);
            ed.putFloat("py_" + i, playerPositions.get(i)[1]);
        }
        ed.apply();
    }

    private void loadFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        teamName = prefs.getString("teamName", "تشكيلة رحومي");
        playerCount = prefs.getInt("playerCount", 11);
        for (int i = 0; i < 11; i++) {
            playerNames.set(i, prefs.getString("name_" + i, i == 0 ? "الحارس" : "لاعب " + (i + 1)));
            playerNumbers.set(i, prefs.getString("num_" + i, String.valueOf(i + 1)));
            playerPhotosBase64.set(i, prefs.getString("photo_" + i, ""));
            float px = prefs.getFloat("px_" + i, PRESET_11[i][0]);
            float py = prefs.getFloat("py_" + i, PRESET_11[i][1]);
            playerPositions.set(i, new float[]{px, py});
        }
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    private static class PitchOverlayView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        PitchOverlayView(Context context) {
            super(context);
            paint.setColor(Color.parseColor("#66FFFFFF"));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            int pad = 28;
            canvas.drawRect(new RectF(pad, pad, w - pad, h - pad), paint);
            canvas.drawLine(pad, h / 2f, w - pad, h / 2f, paint);
            canvas.drawCircle(w / 2f, h / 2f, Math.min(w, h) * 0.14f, paint);
        }
    }
}
