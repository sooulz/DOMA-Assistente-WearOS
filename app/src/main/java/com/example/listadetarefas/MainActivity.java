
package com.example.listadetarefas;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String CANAL_NOTIFICACOES = "doma_avisos";

    private ListView listView;

    private Button btnVerificarAudio;
    private Button btnComandoVoz;
    private Button btnLerMensagem;
    private Button btnAlertaSeguranca;
    private Button btnPermitirNotificacoes;
    private Button btnCriarNotificacao;
    private Button btnConfigurarBluetooth;

    private ArrayList<String> informacoes;
    private ArrayAdapter<String> adapter;

    private AudioManager audioManager;
    private TextToSpeech textToSpeech;
    private boolean vozPronta = false;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private ActivityResultLauncher<Intent> reconhecimentoVoz;

    private final String mensagemEmpresa =
            "Nova mensagem da empresa DOMA. " +
                    "Atenção, funcionários: a reunião de treinamento " +
                    "começará em 15 minutos.";

    private final String mensagemAlerta =
            "Atenção! Este é um alerta de segurança simulado " +
                    "da empresa DOMA. " +
                    "Em caso de emergência, mantenha a calma, " +
                    "siga as orientações da equipe de segurança " +
                    "e dirija-se ao ponto de encontro indicado.";

    private final AudioDeviceCallback audioCallback =
            new AudioDeviceCallback() {

                @Override
                public void onAudioDevicesAdded(
                        AudioDeviceInfo[] devices) {
                    handler.post(() -> verificarAudio());
                }

                @Override
                public void onAudioDevicesRemoved(
                        AudioDeviceInfo[] devices) {
                    handler.post(() -> verificarAudio());
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        btnVerificarAudio = findViewById(R.id.btnVerificarAudio);
        btnComandoVoz = findViewById(R.id.btnComandoVoz);
        btnLerMensagem = findViewById(R.id.btnLerMensagem);
        btnAlertaSeguranca = findViewById(R.id.btnAlertaSeguranca);
        btnPermitirNotificacoes =
                findViewById(R.id.btnPermitirNotificacoes);
        btnCriarNotificacao =
                findViewById(R.id.btnCriarNotificacao);
        btnConfigurarBluetooth =
                findViewById(R.id.btnConfigurarBluetooth);

        audioManager = (AudioManager)
                getSystemService(AUDIO_SERVICE);

        informacoes = new ArrayList<>();

        adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                informacoes
        ) {
            @Override
            public View getView(
                    int position,
                    View convertView,
                    ViewGroup parent) {

                View view = super.getView(
                        position, convertView, parent);

                TextView texto = (TextView) view;
                texto.setTextColor(Color.WHITE);
                texto.setTextSize(12);

                return view;
            }
        };

        listView.setAdapter(adapter);

        reconhecimentoVoz = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                resultado -> {

                    if (resultado.getResultCode() == RESULT_OK
                            && resultado.getData() != null) {

                        ArrayList<String> palavras =
                                resultado.getData()
                                        .getStringArrayListExtra(
                                                RecognizerIntent.EXTRA_RESULTS
                                        );

                        if (palavras != null && !palavras.isEmpty()) {
                            processarComando(palavras.get(0));
                        }
                    }
                }
        );

        textToSpeech = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS) {

                int resultado = textToSpeech.setLanguage(
                        new Locale("pt", "BR")
                );

                vozPronta =
                        resultado != TextToSpeech.LANG_MISSING_DATA &&
                                resultado != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        criarCanalNotificacoes();

        btnVerificarAudio.setOnClickListener(v -> {
            verificarAudio();
            falar("Olá! Eu sou o DOMA. " +
                    "Seu assistente virtual está funcionando.");
        });

        btnComandoVoz.setOnClickListener(v ->
                iniciarReconhecimentoVoz());

        btnLerMensagem.setOnClickListener(v ->
                lerMensagem());

        btnAlertaSeguranca.setOnClickListener(v ->
                emitirAlertaSeguranca());

        btnPermitirNotificacoes.setOnClickListener(v ->
                abrirPermissaoNotificacoes());

        btnCriarNotificacao.setOnClickListener(v ->
                criarNotificacaoTeste());

        // NOVO: ABRIR CONFIGURAÇÕES DE BLUETOOTH
        btnConfigurarBluetooth.setOnClickListener(v ->
                abrirConfiguracoesBluetooth());

        audioManager.registerAudioDeviceCallback(
                audioCallback, handler);

        verificarAudio();
    }

    private void abrirConfiguracoesBluetooth() {

        try {
            Intent intent = new Intent(
                    Settings.ACTION_BLUETOOTH_SETTINGS
            );

            startActivity(intent);

        } catch (ActivityNotFoundException e) {

            Toast.makeText(
                    this,
                    "Configurações de Bluetooth indisponíveis neste relógio.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void criarCanalNotificacoes() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel canal =
                    new NotificationChannel(
                            CANAL_NOTIFICACOES,
                            "Avisos da DOMA",
                            NotificationManager.IMPORTANCE_DEFAULT
                    );

            canal.setDescription(
                    "Mensagens e avisos da empresa DOMA"
            );

            NotificationManager gerenciador =
                    (NotificationManager)
                            getSystemService(NOTIFICATION_SERVICE);

            if (gerenciador != null) {
                gerenciador.createNotificationChannel(canal);
            }
        }
    }

    private void criarNotificacaoTeste() {

        Intent intent = new Intent(
                this, MainActivity.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent abrirAplicativo =
                PendingIntent.getActivity(
                        this, 0, intent, flags);

        String titulo = "Aviso da DOMA";

        String conteudo =
                "A reunião de treinamento começará em 15 minutos.";

        Notification notificacao =
                new NotificationCompat.Builder(
                        this, CANAL_NOTIFICACOES)
                        .setSmallIcon(
                                android.R.drawable.ic_dialog_info)
                        .setContentTitle(titulo)
                        .setContentText(conteudo)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(conteudo))
                        .setContentIntent(abrirAplicativo)
                        .setAutoCancel(true)
                        .setPriority(
                                NotificationCompat.PRIORITY_DEFAULT)
                        .build();

        NotificationManager gerenciador =
                (NotificationManager)
                        getSystemService(NOTIFICATION_SERVICE);

        if (gerenciador != null) {

            gerenciador.notify(1001, notificacao);

            Toast.makeText(
                    this,
                    "Notificação criada!",
                    Toast.LENGTH_SHORT
            ).show();

            falar("Nova notificação. " +
                    titulo + ". " + conteudo);
        }
    }

    private void abrirPermissaoNotificacoes() {

        ComponentName servico = new ComponentName(
                this, DomaNotificationService.class);

        String componentesAtivos = Settings.Secure.getString(
                getContentResolver(),
                "enabled_notification_listeners"
        );

        if (componentesAtivos != null &&
                componentesAtivos.contains(
                        servico.flattenToString())) {

            Toast.makeText(
                    this,
                    "Acesso às notificações já autorizado!",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            startActivity(new Intent(
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));

        } catch (ActivityNotFoundException e) {

            Toast.makeText(
                    this,
                    "Tela de acesso às notificações indisponível neste relógio.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void lerMensagem() {

        Toast.makeText(
                this,
                "Lendo mensagem da DOMA...",
                Toast.LENGTH_SHORT
        ).show();

        falar(mensagemEmpresa);
    }

    private void emitirAlertaSeguranca() {

        Toast.makeText(
                this,
                "ALERTA DE SEGURANÇA - SIMULAÇÃO",
                Toast.LENGTH_LONG
        ).show();

        falar(mensagemAlerta);
    }

    private void iniciarReconhecimentoVoz() {

        Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "pt-BR");

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "pt-BR");

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Fale com o DOMA");

        try {
            reconhecimentoVoz.launch(intent);

        } catch (ActivityNotFoundException e) {

            Toast.makeText(
                    this,
                    "Reconhecimento de voz indisponível",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void processarComando(String comando) {

        String texto = Normalizer.normalize(
                comando.toLowerCase(new Locale("pt", "BR")),
                Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "");

        Toast.makeText(
                this,
                "Você disse: " + comando,
                Toast.LENGTH_LONG
        ).show();

        if (texto.contains("hora") ||
                texto.contains("horario")) {

            SimpleDateFormat formato =
                    new SimpleDateFormat(
                            "HH 'horas e' mm 'minutos'",
                            new Locale("pt", "BR"));

            falar("Agora são " +
                    formato.format(new Date()));

        } else if (texto.contains("alerta") ||
                texto.contains("emergencia") ||
                texto.contains("seguranca")) {

            emitirAlertaSeguranca();

        } else if (texto.contains("notificacao")) {

            criarNotificacaoTeste();

        } else if (texto.contains("mensagem") ||
                texto.contains("recado")) {

            lerMensagem();

        } else if (texto.contains("quem e voce") ||
                texto.contains("seu nome")) {

            falar("Eu sou o DOMA, seu assistente Wear OS.");

        } else if (texto.equals("oi") ||
                texto.contains("ola") ||
                texto.contains("bom dia") ||
                texto.contains("boa tarde") ||
                texto.contains("boa noite")) {

            falar("Olá! Como posso ajudar você?");

        } else if (texto.contains("audio") ||
                texto.contains("alto-falante") ||
                texto.contains("bluetooth")) {

            verificarAudio();

            AudioDeviceInfo[] dispositivos =
                    audioManager.getDevices(
                            AudioManager.GET_DEVICES_OUTPUTS);

            boolean altoFalante = false;
            boolean bluetooth = false;

            for (AudioDeviceInfo dispositivo : dispositivos) {

                if (dispositivo.getType() ==
                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    altoFalante = true;
                }

                if (dispositivo.getType() ==
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                    bluetooth = true;
                }
            }

            falar("Alto-falante " +
                    (altoFalante ? "disponível. " : "indisponível. ") +
                    "Bluetooth " +
                    (bluetooth ? "conectado." : "não conectado."));

        } else {

            falar("Desculpe, não entendi o comando. " +
                    "Tente novamente.");
        }
    }

    private void falar(String mensagem) {

        if (vozPronta && textToSpeech != null) {

            textToSpeech.speak(
                    mensagem,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "DOMA_VOZ"
            );

        } else {

            Toast.makeText(
                    this,
                    "Voz ainda não disponível",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void verificarAudio() {

        if (audioManager == null ||
                informacoes == null ||
                adapter == null) {
            return;
        }

        informacoes.clear();

        AudioDeviceInfo[] dispositivos =
                audioManager.getDevices(
                        AudioManager.GET_DEVICES_OUTPUTS);

        boolean altoFalante = false;
        boolean bluetooth = false;

        for (AudioDeviceInfo dispositivo : dispositivos) {

            if (dispositivo.getType() ==
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                altoFalante = true;
            }

            if (dispositivo.getType() ==
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                bluetooth = true;
            }
        }

        informacoes.add(
                "Alto-falante: " +
                        (altoFalante ? "Disponível" : "Indisponível"));

        informacoes.add(
                "Bluetooth: " +
                        (bluetooth ? "Conectado" : "Não conectado"));

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {

        if (audioManager != null) {
            audioManager.unregisterAudioDeviceCallback(
                    audioCallback);
        }

        handler.removeCallbacksAndMessages(null);

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        super.onDestroy();
    }
}
