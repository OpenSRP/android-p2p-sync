package org.smartregister.p2p.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.smartregister.p2p.R;
import org.smartregister.p2p.contract.P2pModeSelectContract;
import org.smartregister.p2p.dialog.QRCodeGeneratorDialog;
import org.smartregister.p2p.dialog.QRCodeScanningDialog;
import org.smartregister.p2p.dialog.StartDiscoveringModeProgressDialog;
import org.smartregister.p2p.dialog.StartReceiveModeProgressDialog;
import org.smartregister.p2p.handler.OnActivityRequestPermissionHandler;
import org.smartregister.p2p.handler.OnActivityResultHandler;
import org.smartregister.p2p.handler.OnResumeHandler;
import org.smartregister.p2p.presenter.P2PReceiverPresenter;
import org.smartregister.p2p.presenter.P2PSenderPresenter;
import org.smartregister.p2p.util.Constants;
import org.smartregister.p2p.util.DialogUtils;
import org.smartregister.p2p.util.Permissions;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class P2pModeSelectActivity extends AppCompatActivity implements P2pModeSelectContract.View {

    private Button sendButton;
    private Button receiveButton;
    private Button sendMsgBtn;

    private TextView messagesTv;
    private EditText messageToSendEt;

    private P2pModeSelectContract.SenderPresenter senderBasePresenter;
    private P2pModeSelectContract.ReceiverPresenter receiverBasePresenter;

    private ArrayList<OnActivityResultHandler> onActivityResultHandlers = new ArrayList<>();
    private ArrayList<OnResumeHandler> onResumeHandlers = new ArrayList<>();
    private ArrayList<OnActivityRequestPermissionHandler> onActivityRequestPermissionHandlers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_p2p_mode_select);

        sendButton = findViewById(R.id.btn_p2pModeSelectActivity_send);
        receiveButton = findViewById(R.id.btn_p2pModeSelectActivity_receive);
        sendMsgBtn = findViewById(R.id.btn_p2pModeSelectActivity_sendMsgBtn);
        messagesTv = findViewById(R.id.tv_p2pModeSelectActivity_conversationDetails);
        messageToSendEt = findViewById(R.id.et_p2pModeSelectActivity_messageToSend);

        sendMsgBtn = findViewById(R.id.btn_p2pModeSelectActivity_sendMsgBtn);
        messageToSendEt = findViewById(R.id.et_p2pModeSelectActivity_messageToSend);

        sendMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messageToSendEt.getText() != null) {
                    String messageToSend = messageToSendEt.getText().toString();

                    // This is not ideal but for testing purposes for now
                    // It will not be in the any final release
                    receiverBasePresenter.sendTextMessage(messageToSend);
                    senderBasePresenter.sendTextMessage(messageToSend);
                    displayMessage("YOU: " + messageToSend);

                    messageToSendEt.setText("");
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePresenters();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                senderBasePresenter.onSendButtonClicked();
            }
        });
        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiverBasePresenter.onReceiveButtonClicked();
            }
        });
    }

    @Override
    public void enableSendReceiveButtons(boolean enable) {
        sendButton.setEnabled(enable);
        receiveButton.setEnabled(enable);
    }

    @Override
    public void showReceiveProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        StartReceiveModeProgressDialog newFragment = new StartReceiveModeProgressDialog();
        newFragment.setDialogCancelCallback(dialogCancelCallback);

        newFragment.show(fragmentManager, Constants.Dialog.START_RECEIVE_MODE_PROGRESS);
    }

    @Override
    public boolean removeReceiveProgressDialog() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(Constants.Dialog.START_RECEIVE_MODE_PROGRESS);

        if (fragment != null && fragment instanceof DialogFragment) {
            ((DialogFragment) fragment)
                    .dismiss();

            return true;
        }

        return false;
    }

    @Override
    public void showDiscoveringProgressDialog(@NonNull DialogCancelCallback dialogCancelCallback) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        StartDiscoveringModeProgressDialog newFragment = new StartDiscoveringModeProgressDialog();
        newFragment.setDialogCancelCallback(dialogCancelCallback);

        newFragment.show(fragmentManager, Constants.Dialog.START_SEND_MODE_PROGRESS);
    }

    @Override
    public boolean removeDiscoveringProgressDialog() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(Constants.Dialog.START_SEND_MODE_PROGRESS);

        if (fragment != null && fragment instanceof DialogFragment) {
            ((DialogFragment) fragment)
                    .dismiss();

            return true;
        }

        return false;
    }

    @Override
    public void showQRCodeScanningDialog(@NonNull QRCodeScanningDialog.QRCodeScanDialogCallback qrCodeScanDialogCallback) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        QRCodeScanningDialog newFragment = new QRCodeScanningDialog();
        newFragment.setOnQRRecognisedListener(qrCodeScanDialogCallback);

        newFragment.show(fragmentManager, Constants.Dialog.QR_CODE_SCANNING);
    }

    @Override
    public void showQRCodeGeneratorDialog(@NonNull String authenticationCode, @NonNull String deviceName
            , @NonNull QRCodeGeneratorDialog.QRCodeAuthenticationCallback qrCodeAuthenticationCallback) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        QRCodeGeneratorDialog newFragment = new QRCodeGeneratorDialog();
        newFragment.setAuthenticationCode(authenticationCode);
        newFragment.setDeviceName(deviceName);
        newFragment.setQrCodeAuthenticationCallback(qrCodeAuthenticationCallback);

        newFragment.show(fragmentManager, Constants.Dialog.AUTHENTICATION_QR_CODE_GENERATOR);
    }

    @Override
    public void showConnectionAcceptDialog(@NonNull String receiverDeviceName, @NonNull String authenticationCode
            , @NonNull final DialogInterface.OnClickListener onClickListener) {
        new android.support.v7.app.AlertDialog.Builder(this)
                .setTitle(String.format(getString(R.string.accept_connection_dialog_title), receiverDeviceName))
                .setMessage(String.format(getString(R.string.accept_connection_dialog_content), authenticationCode))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onClickListener.onClick(dialog, which);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onClickListener.onClick(dialog, which);
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void requestPermissions(@NonNull List<String> unauthorisedPermissions) {
        Permissions.request(this, unauthorisedPermissions.toArray(new String[]{}), Constants.RqCode.PERMISSIONS);
    }

    @NonNull
    @Override
    public List<String> getUnauthorisedPermissions() {
        return Permissions.getUnauthorizedCriticalPermissions(
                getContext(), Permissions.CRITICAL_PERMISSIONS
        );
    }

    @Override
    public boolean addOnActivityResultHandler(@NonNull OnActivityResultHandler onActivityResultHandler) {
        return !onActivityResultHandlers.contains(onActivityResultHandler)
                && onActivityResultHandlers.add(onActivityResultHandler);
    }

    @Override
    public boolean removeOnActivityResultHandler(@NonNull OnActivityResultHandler onActivityResultHandler) {
        return onActivityResultHandlers.remove(onActivityResultHandler);
    }

    @Override
    public boolean addOnResumeHandler(@NonNull OnResumeHandler onResumeHandler) {
        return !onResumeHandlers.contains(onResumeHandler) && onResumeHandlers.add(onResumeHandler);
    }

    @Override
    public boolean removeOnResumeHandler(@NonNull OnResumeHandler onResumeHandler) {
        return onResumeHandlers.remove(onResumeHandler);
    }

    @Override
    public boolean isLocationEnabled() {
        LocationManager lm = ((LocationManager) getSystemService(Context.LOCATION_SERVICE));
        return lm != null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    @Override
    public void dismissAllDialogs() {
        DialogUtils.dismissAllDialogs(getSupportFragmentManager());
    }

    @Override
    public void requestEnableLocation(@NonNull final OnLocationEnabled onLocationEnabled) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        locationRequest.setInterval(20000);
        locationRequest.setFastestInterval(20000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    onLocationEnabled.locationEnabled();
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;

                                addOnActivityResultHandler(new OnActivityResultHandler() {
                                    @Override
                                    public int getRequestCode() {
                                        return Constants.RqCode.LOCATION_SETTINGS;
                                    }

                                    @Override
                                    public void handleActivityResult(int resultCode, Intent data) {
                                        removeOnActivityResultHandler(this);

                                        if (data != null) {
                                            if (resultCode == Activity.RESULT_OK) {
                                                // All required changes were successfully made
                                                onLocationEnabled.locationEnabled();
                                            } else if (resultCode == Activity.RESULT_CANCELED) {
                                                // The user was asked to change settings, but chose not to
                                                showLocationEnableRejectionDialog();
                                            }
                                        }
                                    }
                                });
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        P2pModeSelectActivity.this,
                                        Constants.RqCode.LOCATION_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                                Timber.e(e);
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                                Timber.e(e);
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            Timber.e("Location settings are not satisfied and we have no way to fix the settings");
                            break;


                        default:
                            // This should almost never happen
                            Timber.e(exception, "The location settings API returned an unresolved status code %d", exception.getStatusCode());
                            break;
                    }
                }
            }
        });
    }

    @Override
    public void showToast(@NonNull String text, int length) {
        Toast.makeText(this, text, length)
                .show();
    }

    @Override
    public void displayMessage(@NonNull String text) {
        String beforeText = messagesTv.getText() != null ? messagesTv.getText().toString() : "";

        // Todo: this should be moved to another method or only called once
        if (!messageToSendEt.isEnabled()) {
            messageToSendEt.setEnabled(true);
            sendMsgBtn.setEnabled(true);
        }

        messagesTv.setText(String.format("%s\n%s", beforeText, text));
    }

    private void showLocationEnableRejectionDialog() {
        new AlertDialog.Builder(P2pModeSelectActivity.this)
                .setTitle(R.string.location_service_disabled)
                .setMessage(R.string.file_data_sharing_will_not_work_without_location)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        for (OnResumeHandler onResumeHandler: onResumeHandlers) {
            onResumeHandler.onResume();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (OnActivityRequestPermissionHandler onActivityRequestPermissionHandler: onActivityRequestPermissionHandlers) {
            if (requestCode == onActivityRequestPermissionHandler.getRequestCode()) {
                onActivityRequestPermissionHandler.handlePermissionResult(permissions, grantResults);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for (OnActivityResultHandler onActivityResultHandler: onActivityResultHandlers) {
            if (requestCode == onActivityResultHandler.getRequestCode()) {
                onActivityResultHandler.handleActivityResult(resultCode, data); getString(R.string.connected);
            }
        }
    }

    @NonNull
    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void initializePresenters() {
        receiverBasePresenter = new P2PReceiverPresenter(this);
        senderBasePresenter = new P2PSenderPresenter(this);
    }

    @Override
    public boolean addOnActivityRequestPermissionHandler(@NonNull OnActivityRequestPermissionHandler onActivityRequestPermissionHandler) {
        return !onActivityRequestPermissionHandlers.contains(onActivityRequestPermissionHandler)
                && onActivityRequestPermissionHandlers.add(onActivityRequestPermissionHandler);
    }

    @Override
    public boolean removeOnActivityRequestPermissionHandler(@NonNull OnActivityRequestPermissionHandler onActivityRequestPermissionHandler) {
        return onActivityRequestPermissionHandlers.remove(onActivityRequestPermissionHandler);
    }

    @Override
    protected void onStop() {
        super.onStop();

        messageToSendEt.setEnabled(false);
        sendMsgBtn.setEnabled(false);

        sendButton.setOnClickListener(null);
        receiveButton.setOnClickListener(null);

        receiverBasePresenter.onStop();
        senderBasePresenter.onStop();
    }


}
