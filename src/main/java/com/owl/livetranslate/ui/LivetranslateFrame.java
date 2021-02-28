package com.owl.livetranslate.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

import com.owl.livetranslate.bean.receiver.DanmuInfo;
import com.owl.livetranslate.network.receiver.DamuReceiver;
import com.owl.livetranslate.network.receiver.DamuReceiverClient;
import com.owl.livetranslate.network.sender.DamuSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class LivetranslateFrame extends JFrame {

    @Autowired
    private DamuSender damuSender;

    @Autowired
    private DamuReceiver damuReceiver;

    @Autowired
    private ExecutorService executorService;

    // 窗口宽度
    public static final int WIDTH = 450;
    // 窗口高度
    public static final int HEIGHT = 400;
    private JTextArea sendTextArea;
    private boolean hasSettedSetting;
    private String speaker;
    private String[] cookieds;
    private String[] csrfs;
    private int curCookiedIdx;
    private long lastTimeOfUseCookied;

    private int[] roomids;
    private JTextArea logTextArea;

    private Integer readRoomId;
    private LinkedBlockingQueue<DanmuInfo> danmuQues;
    private Pattern targetDanmuPattern;
    private volatile boolean readingDanmu;
    private Future<?> dealDamuTask;
    private DamuReceiverClient danmuclientReader;

    public LivetranslateFrame() {

        // 设置标题
        setTitle("联动直播同传广播工具-(喜欢的关注御剑莉亚)");
        // 设置大小
        setSize(WIDTH, HEIGHT);
        // 关闭窗口
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        //置顶
        setAlwaysOnTop(true);


        initPanelCont();
        // 设置窗口屏幕居中
        setLocationRelativeTo(null);
        // 设置可见
        setVisible(true);
    }

    private void initPanelCont() {
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout());
        setContentPane(rootPanel);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        rootPanel.add(mainPanel, BorderLayout.CENTER);


        JPanel roomInfoPanel = new JPanel();
        mainPanel.add(roomInfoPanel, BorderLayout.NORTH);
        roomInfoPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 10, 5));
        JLabel roomidLabel = new JLabel("房间id列表(逗号分隔)");
        roomInfoPanel.add(roomidLabel);

        JTextField roomidTextField = new JTextField();
        roomidTextField.setPreferredSize(new Dimension(100, 20));
        roomInfoPanel.add(roomidTextField);

        JLabel speakerLabel = new JLabel("说话人");
        roomInfoPanel.add(speakerLabel);

        JTextField speakerTextField = new JTextField();
        speakerTextField.setPreferredSize(new Dimension(50, 20));
        roomInfoPanel.add(speakerTextField);

        JPanel damuInfoPanel = new JPanel();
        damuInfoPanel.setLayout(new BorderLayout());
        mainPanel.add(damuInfoPanel, BorderLayout.CENTER);

        JPanel cookiedInfoPanel = new JPanel();
        cookiedInfoPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 10, 5));
        damuInfoPanel.add(cookiedInfoPanel, BorderLayout.NORTH);

        JLabel cookiedLabel = new JLabel("cookied(用'@!@'分割)");
        cookiedInfoPanel.add(cookiedLabel);

        JTextArea cookiedTextArea = new JTextArea();
        cookiedTextArea.setLineWrap(true);

        JScrollPane cookiedScrollPanel = new JScrollPane(cookiedTextArea);
        cookiedScrollPanel.setPreferredSize(new Dimension(150, 50));
        cookiedInfoPanel.add(cookiedScrollPanel);

        JButton settingBtn = new JButton("设置完成");
        cookiedInfoPanel.add(settingBtn);
        settingBtn.addActionListener(e -> {
            if (hasSettedSetting) {
                roomidTextField.setEnabled(true);
                speakerTextField.setEnabled(true);
                cookiedTextArea.setEnabled(true);
                settingBtn.setText("设置完成");
                sendTextArea.setEnabled(false);
                hasSettedSetting = false;
                return;
            }


            if (!StringUtils.hasText(roomidTextField.getText())) {
                showErrMsg("请输入房间id");
                return;
            }

            if (!StringUtils.hasText(cookiedTextArea.getText())) {
                showErrMsg("请输入cookied");
                return;
            }

            String[] roomidStrs = roomidTextField.getText().split(",");
            roomids = new int[roomidStrs.length];
            for (int i = 0; i < roomidStrs.length; i++) {
                Integer roomid = changeStrToInt(roomidStrs[i]);
                if(roomid == null){
                    showErrMsg("房间号错误");
                    return;
                }

                roomids[i] = roomid;
            }

            roomidTextField.setEnabled(false);
            speakerTextField.setEnabled(false);
            cookiedTextArea.setEnabled(false);
            settingBtn.setText("修改设置");
            sendTextArea.setEnabled(true);
            hasSettedSetting = true;

            speaker = speakerTextField.getText();
            cookieds = cookiedTextArea.getText().split("@!@");
            csrfs = new String[cookieds.length];
            for (int i = 0; i < cookieds.length; i++) {
                csrfs[i] = damuSender.getCsrfByCookied(cookieds[i]);
            }
        });


        JPanel sendInfoPanel = new JPanel();
        sendInfoPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 10, 5));
        damuInfoPanel.add(sendInfoPanel, BorderLayout.CENTER);

        JLabel sendLabel = new JLabel("发送内容(回车发送)");
        sendInfoPanel.add(sendLabel);

        sendTextArea = new JTextArea();
        sendTextArea.setLineWrap(true);
        sendTextArea.setEnabled(false);
        enterPressesWhenFocused(sendTextArea, e->{
            String msg = sendTextArea.getText();
            msg = msg.substring(0, msg.length() - 1);
            sendTextArea.setText("");
            if(!StringUtils.hasText(msg)){
                return;
            }

            String sendMsg = msg;
            //异步发信息到房间
            sendMsgAsyn(sendMsg, false);
        });


        JScrollPane sendScrollPanel = new JScrollPane(sendTextArea);
        sendScrollPanel.setPreferredSize(new Dimension(250, 50));
        sendInfoPanel.add(sendScrollPanel);


        //读取弹幕
        JLabel readDanmuRoodIdLaebl = new JLabel("读取弹幕房间号");
        sendInfoPanel.add(readDanmuRoodIdLaebl);
        JTextField readDanmuRoodIdField = new JTextField();
        readDanmuRoodIdField.setPreferredSize(new Dimension(50, 20));
        sendInfoPanel.add(readDanmuRoodIdField);

        JLabel targetDanmuRegxLabel = new JLabel("抓取弹幕格式");
        sendInfoPanel.add(targetDanmuRegxLabel);
        JTextField targetDanmuRegxField = new JTextField("【.*】");
        targetDanmuRegxField.setPreferredSize(new Dimension(50, 20));
        sendInfoPanel.add(targetDanmuRegxField);

        JButton readDanmuStartBtn = new JButton("开始读取翻译");
        readDanmuStartBtn.addActionListener( e -> {
            if(readingDanmu){
                stopReadDanmu();

                readDanmuStartBtn.setText("开始读取翻译");
                return;
            }

            if (!StringUtils.hasText(readDanmuRoodIdField.getText())) {
                showErrMsg("请输入读取弹幕房间号");
                return;
            }

            readRoomId = changeStrToInt(readDanmuRoodIdField.getText());
            if(readRoomId == null){
                showErrMsg("读取弹幕房间号错误");
                return;
            }

            if (!StringUtils.hasText(targetDanmuRegxField.getText())) {
                showErrMsg("请输入抓取弹幕格式");
                return;
            }

            if(!hasSettedSetting){
                showErrMsg("请先设置好cookied信息");
                return;
            }



            String targetDanmuRegxStr = targetDanmuRegxField.getText();
            targetDanmuPattern = Pattern.compile(targetDanmuRegxStr);

            readDanmuStartBtn.setText("停止读取");
            readingDanmu = true;
            startReadRoom();
        });
        sendInfoPanel.add(readDanmuStartBtn);


        //日记
        logTextArea = new JTextArea();
        logTextArea.setLineWrap(true);
        JScrollPane logScrollPanel = new JScrollPane(logTextArea);
        logScrollPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED); 
        Dimension logScrollPanelSize = logScrollPanel.getPreferredSize();
        logScrollPanelSize.height = 50;
        logScrollPanel.setPreferredSize(logScrollPanelSize);
        damuInfoPanel.add(logScrollPanel, BorderLayout.SOUTH);
    }

    private void sendMsgAsyn(String sendMsg, boolean isRaw) {
        executorService.execute(() ->{
            for (int i = 0; i < roomids.length; i++) {
                try{
                    int roomid = roomids[i];
                    int nextCookiedIdx = getNextCookiedIdx();
                    if(isRaw){
                        damuSender.sendDamuRaw(roomid, sendMsg, cookieds[nextCookiedIdx], csrfs[nextCookiedIdx]);
                    }else{
                        damuSender.sendDamu(roomid, sendMsg, cookieds[nextCookiedIdx], csrfs[nextCookiedIdx], speaker);
                    }

                    addLog(String.format("roomId:%s， 内容:%s, 发送成功", roomid, sendMsg));
                }catch (Exception exception){
                    addLog("发送失败:" + exception.getMessage());
                }
            }

        });
    }

    private synchronized int getNextCookiedIdx() throws InterruptedException {
        int result = curCookiedIdx;
        ++curCookiedIdx;
        if(curCookiedIdx >= cookieds.length){
            curCookiedIdx = 0;
        }

        if(result == 0){
            //要距离上一次使用的间隔大于1秒
            long diff = System.currentTimeMillis() - lastTimeOfUseCookied;
            if(diff < 1100){
                TimeUnit.MILLISECONDS.sleep(diff);
            }
            lastTimeOfUseCookied = System.currentTimeMillis();
        }

        return result;
    }

    private synchronized void startReadRoom(){
        if(danmuQues == null){
            danmuQues = new LinkedBlockingQueue<DanmuInfo>();
        }
        danmuQues.clear();

        damuReceiver.startListenToRoom(readRoomId, logMsg ->{
                                dealDamuReceiverLog(logMsg);
                            }, client ->{
                                dealStartDamuSucess(client);
                            }, danmu ->{
                                dealDanmu(danmu);
                            },
                            ctx ->{
                                dealDamuReceiverDisconnect();
                            });
    }

    private synchronized void dealStartDamuSucess(DamuReceiverClient client) {
        danmuclientReader = client;
        dealDamuTask = executorService.submit(() -> {
            while (readingDanmu) {
                try {
                    DanmuInfo danmuInfo = danmuQues.poll(1, TimeUnit.SECONDS);
                    if(!readingDanmu){
                        break;
                    }

                    if(danmuInfo == null){
                        continue;
                    }

                    String content = danmuInfo.getContent();
                    if (!StringUtils.hasText(content)) {
                        continue;
                    }

                    Matcher matcher = targetDanmuPattern.matcher(content);
                    if (!matcher.find()) {
                        continue;
                    }

                    sendMsgAsyn(content, true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private synchronized void stopReadDanmu(){
        danmuclientReader.stop();
        readingDanmu = false;
        danmuQues.clear();
        dealDamuTask.cancel(true);
        dealDamuTask = null;
    }

    private synchronized void dealDamuReceiverDisconnect() {
        SwingUtilities.invokeLater(() ->{
            addLog("掉线");
        });

        if(readingDanmu){
            stopReadDanmu();
            //重新连接
            SwingUtilities.invokeLater(() ->{
                addLog("重新连接...");
            });
            startReadRoom();
        }
    }

    private void dealDamuReceiverLog(String logMsg) {
        SwingUtilities.invokeLater(() ->{
            addLog(logMsg);    
        });
    }

    private Integer changeStrToInt(String str){
        try{
            return Integer.parseInt(str);
        }catch (Exception e){

        }
        return null;
    }

    /**
     * 处理弹幕信息
     * @param danmu
     */
    private void dealDanmu(DanmuInfo danmu) {
        danmuQues.offer(danmu);
    }

    private void addLog(String msg){
        if(logTextArea.getText().length() > 10000){
            logTextArea.setText("");
        }
        logTextArea.append(msg + "\n");
        logTextArea.setCaretPosition(logTextArea.getText().length());
    }

    private void enterPressesWhenFocused(JTextArea textField, ActionListener actionListener) {
        textField.registerKeyboardAction(actionListener,KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), JComponent.WHEN_FOCUSED);
    }

    private void showErrMsg(String msg) {
        JOptionPane.showMessageDialog(null, msg, "错误", JOptionPane.ERROR_MESSAGE);
    }
}
