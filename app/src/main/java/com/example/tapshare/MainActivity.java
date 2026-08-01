package com.example.tapshare;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private EditText nameInput;
    private EditText phoneInput;
    private EditText emailInput;
    private TextView statusView;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        buildUi();
        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
        refreshStatus("Ready. Keep Tap Share open, unlock the other phone, and tap an NFC tag or another device that sends a vCard.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Tap Share");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView explanation = new TextView(this);
        explanation.setText("Android no longer supports silent phone-to-phone NFC sharing. This prototype prepares your contact as a vCard and imports a vCard received over NFC when the app is open.");
        explanation.setPadding(0, 24, 0, 24);
        root.addView(explanation);

        nameInput = field("Name", "Alex Tapshare");
        phoneInput = field("Phone", "+1 555 010 1234");
        emailInput = field("Email", "alex@example.com");
        root.addView(nameInput);
        root.addView(phoneInput);
        root.addView(emailInput);

        Button previewButton = new Button(this);
        previewButton.setText("Preview my share card");
        previewButton.setOnClickListener(v -> refreshStatus(buildVCard()));
        root.addView(previewButton);

        Button importButton = new Button(this);
        importButton.setText("Open contact import screen");
        importButton.setOnClickListener(v -> importVCard(buildVCard()));
        root.addView(importButton);

        statusView = new TextView(this);
        statusView.setPadding(0, 24, 0, 0);
        root.addView(statusView);
        setContentView(scrollView);
    }

    private EditText field(String hint, String text) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setText(text);
        editText.setSingleLine(true);
        return editText;
    }

    private String buildVCard() {
        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        return "BEGIN:VCARD\nVERSION:3.0\nFN:" + escape(name) + "\nTEL:" + escape(phone) + "\nEMAIL:" + escape(email) + "\nEND:VCARD";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }

    private void handleIntent(Intent intent) {
        if (intent == null || !NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            return;
        }
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages == null || rawMessages.length == 0) {
            refreshStatus("No contact data found on this NFC tap.");
            return;
        }
        for (Parcelable rawMessage : rawMessages) {
            NdefMessage message = (NdefMessage) rawMessage;
            for (NdefRecord record : message.getRecords()) {
                String type = new String(record.getType(), StandardCharsets.US_ASCII);
                if ("text/vcard".equals(type) || "text/x-vcard".equals(type)) {
                    String vCard = new String(record.getPayload(), StandardCharsets.UTF_8);
                    importVCard(vCard);
                    return;
                }
            }
        }
        refreshStatus("Tapped NFC data was not a vCard contact.");
    }

    private void importVCard(String vCard) {
        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.NAME, extract(vCard, "FN:"));
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, extract(vCard, "TEL:"));
        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, extract(vCard, "EMAIL:"));
        try {
            startActivity(intent);
        } catch (Exception exception) {
            Toast.makeText(this, "No contacts app available to import this card.", Toast.LENGTH_LONG).show();
        }
    }

    private String extract(String vCard, String prefix) {
        for (String line : vCard.split("\\n")) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).replace("\\n", "\n").replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\");
            }
        }
        return "";
    }

    private void refreshStatus(String status) {
        if (statusView != null) {
            statusView.setText(status);
        }
    }
}
