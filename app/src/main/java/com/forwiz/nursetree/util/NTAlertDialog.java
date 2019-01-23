package com.forwiz.nursetree.util;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;

import com.forwiz.nursetree.R;

import java.util.Locale;


/**
 * WithLearn 어플리케이션에서 사용자에게 안내메세지, 혹은 선택사항을 안내하는 메세지를
 * 표시하는 유틸리티 클래스입니다.
 */
public class NTAlertDialog {

    static AlertDialog thisDialog;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void showDialog(final AlertDialog.Builder target) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            target.show();
            return;
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                target.show();
            }
        });
    }

    public static AlertDialog.Builder getSystemErrorAnnounce(Context context) {
        return getSystemErrorAnnounce(context, simpleCancelListener);
    }



    public static AlertDialog.Builder getSystemErrorAnnounce(Context context,
                                                             DialogInterface.OnClickListener comfirmListener) {

        return getBuilderImpl(context)
                .setIcon(R.drawable.app_icon_a)
                .setTitle(R.string.nt_system_error_title)
                .setMessage(R.string.nt_system_error_msg)
                .setPositiveButton(R.string.common_confirm_text, comfirmListener);
    }

    /**
     * 사용자에게 특정 메세지를 표시하는 기능만을 가진 메세지창을 표시합니다.
     * 메세지창의 제목과 확인버튼이 기본적으로 포함되어있으며, 이를 선택하면 다이얼로그를 cancel()하는 기본 내장된 {@link #simpleCancelListener}가 실행됩니다.
     * @param context Dialog을 표시하는 Context
     * @param msgRes 메세지창의 내용을 가리키는 Resource id
     * @return {@link AlertDialog} 객체가 반환되며, 이를 이용해 show() 메소드를 실행하여 질의창을 표시할 수 있습니다.
     */
    public static AlertDialog.Builder getSimpleAnnounce(Context context, int msgRes) {
        return getSimpleAnnounce(context, -1, msgRes);
    }

    /**
     * 사용자에게 특정 메세지를 표시하는 기능만을 가진 메세지창을 표시합니다.
     * 확인버튼이 기본적으로 포함되어있으며, 이를 선택하면 다이얼로그를 cancel()하는 기본 내장된 {@link #simpleCancelListener}가 실행됩니다.
     * @param context Dialog을 표시하는 Context
     * @param titleRes 메세지창의 제목을 가리키는 Resource id
     * @param msgRes 메세지창의 내용을 가리키는 Resource id
     * @return {@link AlertDialog} 객체가 반환되며, 이를 이용해 show() 메소드를 실행하여 질의창을 표시할 수 있습니다.
     */
    public static AlertDialog.Builder getSimpleAnnounce(Context context,
                                                        int titleRes,
                                                        int msgRes) {

        AlertDialog.Builder builder = getBuilderImpl(context)
                .setMessage(msgRes)
                .setPositiveButton(R.string.common_confirm_text, simpleCancelListener);

        if (titleRes > -1) {
            builder.setTitle(titleRes);
        }

        return builder;
    }



    /**
     * 확인, 취소의 버튼을 가지는 메세지창을 사용자에게 표시합니다.
     * 확인을 선택하면 사용자가 지정한 {@link DialogInterface.OnClickListener}가 실행되며,
     * 취소를 선택하면 다이얼로그를 cancel()하는 기본 내장된 {@link #simpleCancelListener}가 실행됩니다.
     * @param context Dialog을 표시하는 Context
     * @param titleRes 메세지창의 제목을 가리키는 Resource id
     * @param msgRes 메세지창의 내용을 가리키는 Resource id
     * @param comfirmListener 확인을 선택하였을 때 실행할 로직이 포함된 {@link DialogInterface.OnClickListener}
     * @return {@link AlertDialog} 객체가 반환되며, 이를 이용해 show() 메소드를 실행하여 질의창을 표시할 수 있습니다.
     */

    public static AlertDialog.Builder getOneButtonDialog(Context context,
                                                         int titleRes,
                                                         int msgRes,
                                                         int button1Id,
                                                         DialogInterface.OnClickListener confirmListener) {
        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(button1Id, confirmListener);
    }

    public static AlertDialog.Builder getTwoButtonDialog(Context context,
                                                         int titleRes,
                                                         int msgRes,
                                                         int button1Id,
                                                         int button2Id,
                                                         DialogInterface.OnClickListener comfirmListener,
                                                         DialogInterface.OnClickListener cancelListener) {
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(button1Id, comfirmListener)
                .setNegativeButton(button2Id, cancelListener);
    }

    public static AlertDialog.Builder getComfirmCancelDialog(Context context,
                                                             int titleRes,
                                                             int msgRes,
                                                             DialogInterface.OnClickListener comfirmListener, DialogInterface.OnClickListener cancelListener) {

        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(R.string.common_confirm_text, comfirmListener)
                .setNegativeButton(R.string.common_cancel_text, cancelListener);
    }

    public static AlertDialog.Builder getPurchaseCancelDialog(Context context,
                                                              int titleRes,
                                                              String msgRes,
                                                              DialogInterface.OnClickListener comfirmListener, DialogInterface.OnClickListener cancelListener) {

        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(R.string.nt_purchase_dialog_confirm, comfirmListener)
                .setNegativeButton(R.string.common_cancel_text, cancelListener);
    }


    public static AlertDialog.Builder getConfirmCancelDialog(Context context,
                                                             int titleRes,
                                                             String msgRes,
                                                             DialogInterface.OnClickListener comfirmListener, DialogInterface.OnClickListener cancelListener) {

        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(R.string.common_confirm_text, comfirmListener)
                .setNegativeButton(R.string.common_cancel_text, cancelListener);
    }


    public static AlertDialog.Builder getCancelScheduleDialog(Context context,
                                                              int titleRes,
                                                              int msgRes,
                                                              DialogInterface.OnClickListener comfirmListener, DialogInterface.OnClickListener cancelListener) {

        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(R.string.nt_cancel_schedule_okay, comfirmListener)
                .setNegativeButton(R.string.nt_cancel_schedule_change_mind, cancelListener);
    }
    public AlertDialog.Builder getNonStaticScheduleCancelDialog(Context context,
                                                                int titleRes,
                                                                int msgRes,
                                                                DialogInterface.OnClickListener comfirmListener, DialogInterface.OnClickListener cancelListener) {

        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(R.string.nt_cancel_schedule_okay, comfirmListener)
                .setNegativeButton(R.string.nt_cancel_schedule_change_mind, cancelListener);
    }



    public AlertDialog.Builder getNonStaticConfirmCancelDialog(Context context,
                                                               int titleRes,
                                                               int msgRes,
                                                               DialogInterface.OnClickListener comfirmListener, DialogInterface.OnClickListener cancelListener) {

        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(R.string.common_confirm_text, comfirmListener)
                .setNegativeButton(R.string.common_cancel_text, cancelListener);
    }


//    public static AlertDialog.Builder getScheduleDialog(Context context,
//                                                       int titleRes,
//                                                       DialogInterface.OnClickListener comfirmListener) {
//
//        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
//        return getBuilderImpl(context)
//                .setTitle(titleRes)
//                .setPositiveButton(R.string.common_cancel_text, comfirmListener);
//    }


    public  AlertDialog.Builder getNotiLearnDialog(Context context,
                                                   int titleRes,
                                                   int msgRes,
                                                   DialogInterface.OnClickListener comfirmListener, DialogInterface.OnClickListener cancelListener) {

        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(R.string.common_confirm_text, comfirmListener)
                .setNegativeButton(R.string.common_cancel_text, cancelListener);
    }


    public static AlertDialog.Builder getForgotPassConfirmCancelDialog(Context context,
                                                                       int titleRes,
                                                                       int msgRes,
                                                                       String email,
                                                                       DialogInterface.OnClickListener confirmListener) {


        Locale systemLocale = context.getResources().getConfiguration().locale;
        String strLanguage = systemLocale.getLanguage();
        if(strLanguage.equals("ko")) {
            // 타이틀, 메세지, 지정된 예, 아니오의 버튼
            return getBuilderImpl(context)
                    .setTitle(titleRes)
                    .setMessage(email + "\n"+ context.getResources().getString(msgRes))
                    .setPositiveButton(R.string.common_confirm_text, confirmListener)
                    .setNegativeButton(R.string.common_cancel_text, simpleCancelListener);
        }
        else{
            // 타이틀, 메세지, 지정된 예, 아니오의 버튼
            return getBuilderImpl(context)
                    .setTitle(titleRes)
                    .setMessage(context.getResources().getString(msgRes) + "\n"+email)
                    .setPositiveButton(R.string.common_confirm_text, confirmListener)
                    .setNegativeButton(R.string.common_cancel_text, simpleCancelListener);
        }
    }


    public static AlertDialog.Builder getUpdateConfirmCancelDialog(Context context,
                                                                   String titleRes,
                                                                   String msgRes,
                                                                   DialogInterface.OnClickListener confirmListener) {

        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(R.string.common_update_text, confirmListener)
                .setNegativeButton(R.string.common_later_text, simpleCancelListener);
    }

    public static AlertDialog.Builder getComfirmDialog(Context context,
                                                       int titleRes,
                                                       int msgRes,
                                                       DialogInterface.OnClickListener comfirmListener) {

        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(R.string.common_confirm_text, comfirmListener);
    }

    public static AlertDialog.Builder getOkDialog(Context context,
                                                  int titleRes,
                                                  int msgRes,
                                                  DialogInterface.OnClickListener comfirmListener) {

        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(R.string.common_okay_text, comfirmListener);
    }



//    public  AlertDialog.Builder getNoOpponentComfirmDialog(Context context,
//                                                       int titleRes,
//                                                       int msgRes,
//                                                       DialogInterface.OnClickListener comfirmListener) {
//
//        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
//        return getBuilderImpl(context)
//                .setTitle(titleRes)
//                .setMessage(msgRes)
//                .setPositiveButton(R.string.common_cancel_text, comfirmListener);
//    }



    public static AlertDialog.Builder getComfirmDialog(Context context,
                                                       int titleRes,
                                                       String msgRes,
                                                       DialogInterface.OnClickListener comfirmListener) {

        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(R.string.common_confirm_text, comfirmListener);
    }

    public static AlertDialog.Builder getComfirmDialog(Context context,
                                                       String titleRes,
                                                       String msgRes,
                                                       DialogInterface.OnClickListener comfirmListener) {

        // 타이틀, 메세지, 지정된 예, 아니오의 버튼
        return getBuilderImpl(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setPositiveButton(R.string.common_confirm_text, comfirmListener);
    }


//    public static AlertDialog.Builder getDefaultWithConfirm(Context context) {
//        return getBuilderImpl(context)
//                .setNegativeButton(R.string.common_cancel_text, simpleCancelListener);
//    }
//
//    public static AlertDialog.Builder getDefaultWithCancel(Context context) {
//        return getBuilderImpl(context)
//                .setNegativeButton(R.string.common_cancel_text, simpleCancelListener);
//    }
//
//    public static AlertDialog.Builder getDefaultBuilder(Context context) {
//        return getBuilderImpl(context);
//    }

    public static final DialogInterface.OnClickListener simpleCancelListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
        }
    };

    private static AlertDialog.Builder getBuilderImpl(Context context) {
        return new AlertDialog.Builder(context)
                .setIcon(R.drawable.app_icon_a)
                .setTitle(R.string.app_name);
    }

//    private static AlertDialog.Builder getBuilderImpl(Context context,boolean existIcon) {
//        return new AlertDialog.Builder(context)
//                .setTitle(R.string.app_name);
//    }
}
