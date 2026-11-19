
package com.example.listadetarefas;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import android.util.Log;

public class DomaNotificationService
        extends NotificationListenerService {

    @Override
    public void onNotificationPosted(
            StatusBarNotification notificacao) {

        super.onNotificationPosted(notificacao);

        if (notificacao == null ||
                notificacao.getNotification() == null) {
            return;
        }

        Bundle dados =
                notificacao.getNotification().extras;

        if (dados == null) {
            return;
        }

        CharSequence titulo = dados.getCharSequence(
                Notification.EXTRA_TITLE
        );

        CharSequence mensagem = dados.getCharSequence(
                Notification.EXTRA_TEXT
        );

        Log.d(
                "DOMA_NOTIFICACAO",
                "Aplicativo: " + notificacao.getPackageName()
                        + " | Título: " + titulo
                        + " | Mensagem: " + mensagem
        );
    }

    @Override
    public void onNotificationRemoved(
            StatusBarNotification notificacao) {

        super.onNotificationRemoved(notificacao);
    }
}
