package com.owl.livetranslate.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owl.livetranslate.bean.receiver.DanmuInfo;
import com.owl.livetranslate.bean.setting.SettingInfo;
import com.owl.livetranslate.network.receiver.DamuReceiver;
import com.owl.livetranslate.network.receiver.DamuReceiverClient;
import com.owl.livetranslate.network.sender.DamuSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class LivetranslateFrame extends JFrame implements InitializingBean {


    public static final String SETTING_FILE_NAME = "livetranslate_owl_setting";
    @Autowired
    private DamuSender damuSender;

    @Autowired
    private DamuReceiver damuReceiver;

    @Autowired
    private ExecutorService executorService;

    // B站最长弹幕长度
    public static final int DANMU_MAX_SEND_LENGTH = 15;
    // 获取cookied的超时时间
    public static final int GET_COOKIED_INDEX_TIMEOUT = 1200;
    //重试发弹幕次数
    public static final int TRY_SEND_DAMNU_COUNT = 5;

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
    private LinkedBlockingQueue<String> danmuQues;
    private Pattern targetDanmuPattern;
    private volatile boolean readingDanmu;
    private Future<?> dealDamuTask;
    private DamuReceiverClient danmuclientReader;
    private boolean pauseDanmu;
    private JTextField roomidTextField;
    private JTextArea cookiedTextArea;
    private JButton settingBtn;

    public LivetranslateFrame() {

        // 设置标题
        setTitle("owl-直播同传广播工具 有问题联系QQ:2284556462");
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

    /**
     * 加载设置文件的内容
     */
    private void loadSettingInfo() {
        Path path = Paths.get(SETTING_FILE_NAME);
        if(!Files.exists(path)){
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(path);
            if(bytes.length <= 0){
                return;
            }

            String content = new String(bytes, StandardCharsets.UTF_8);
            if(!StringUtils.hasText(content)){
                return;
            }

            System.out.println(content);
            SettingInfo settingInfo = new ObjectMapper().readValue(content, SettingInfo.class);

            roomidTextField.setText(settingInfo.getRoomId());
            cookiedTextArea.setText(settingInfo.getCookie());

            settingBtn.doClick();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        roomidTextField = new JTextField();
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

        cookiedTextArea = new JTextArea();
        cookiedTextArea.setLineWrap(true);

        JScrollPane cookiedScrollPanel = new JScrollPane(cookiedTextArea);
        cookiedScrollPanel.setPreferredSize(new Dimension(150, 50));
        cookiedInfoPanel.add(cookiedScrollPanel);

        settingBtn = new JButton("设置完成");
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

            //保存设置到文件
            saveSettingInfoToFile();
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

            //异步发信息到房间
            sendMsgAsyn(msg, false);
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

        JButton readDanmuPauseBtn = new JButton("暂停");
        sendInfoPanel.add(readDanmuPauseBtn);
        readDanmuPauseBtn.addActionListener( e -> {
            if(isPauseDanmu()){
                setPauseDanmu(false);
                readDanmuPauseBtn.setText("暂停");
                return;
            }

            setPauseDanmu(true);
            readDanmuPauseBtn.setText("开始");
        });



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

    /**
     * 保存当前设置到文件，之后启动时直接读取
     */
    private void saveSettingInfoToFile(){
        SettingInfo settingInfo = SettingInfo.builder()
                                            .cookie(cookiedTextArea.getText())
                                            .roomId(roomidTextField.getText()).build();

        try {
            saveContentToFile(new ObjectMapper().writeValueAsString(settingInfo), SETTING_FILE_NAME);
        } catch (JsonProcessingException e) {
            showErrMsg("setting info转json失败");
            e.printStackTrace();
        } catch (IOException e) {
            showErrMsg("setting info保存文件失败");
            e.printStackTrace();
        }
    }

    /**
     * 保存内容到文件
     * @param content
     * @param settingFileName
     * @throws IOException
     */
    private void saveContentToFile(String content, String settingFileName) throws IOException {
        Files.write(Paths.get(settingFileName), content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 只能串行发送
     */
    private synchronized void sendMsgWithTry(String sendMsg, boolean isRaw) throws InterruptedException {
        String[] msgs = splitMessage(sendMsg);
        if(null == msgs){
            return;
        }

        for (String messge : msgs) {
            for (int i = 0; i < roomids.length; i++) {
                int roomid = roomids[i];
                int tryCount = TRY_SEND_DAMNU_COUNT;
                String responseMsg = null;
                while(tryCount > 0){
                    int nextCookiedIdx = getNextCookiedIdx();
                    if (isRaw) {
                        responseMsg = damuSender.sendDamuRaw(roomid, messge, cookieds[nextCookiedIdx], csrfs[nextCookiedIdx]);
                    } else {
                        responseMsg = damuSender.sendDamu(roomid, messge, cookieds[nextCookiedIdx], csrfs[nextCookiedIdx], speaker);
                    }
                    addLog("responseMsg:" + responseMsg);

                    //msg in 1s
                    //发送失败
                    if("msg in 1s".equals(responseMsg) || "msg repeat".equals(responseMsg)){
                        addLog(String.format("roomId:%s， 内容:%s, 发送失败，重试%s", roomid, messge, tryCount));
                        --tryCount;
                        if("msg repeat".equals(responseMsg)){
                            //重复时稍稍改变一下内容
                            messge = messge + tryCount;
                        }

                        TimeUnit.MILLISECONDS.sleep(500);
                        continue;
                    }

                    addLog(String.format("roomId:%s， 内容:%s, 发送成功", roomid, messge));
                    break;
                }
            }
        }
    }

    private void sendMsgAsyn(String sendMsg, boolean isRaw) {
        executorService.execute(() -> {
            try {
                sendMsgWithTry(sendMsg, isRaw);
            } catch (Exception exception) {
                addLog("发送失败:" + exception.getMessage());
            }
        });
    }

    /**
     * B站每次只能发送一定字数，因此太长分开发送
     * @param sendMsg
     * @return
     */
    private String[] splitMessage(String sendMsg) {
        if(null == sendMsg || !StringUtils.hasText(sendMsg)){
            return null;
        }

        int sendMsgLength = sendMsg.length();
        if (sendMsgLength < DANMU_MAX_SEND_LENGTH) {
            return new String[]{sendMsg};
        }

        int count = (int) Math.ceil(((float) sendMsgLength) / (float) DANMU_MAX_SEND_LENGTH);
        String[] messges = new String[count];
        for (int i = 0; i < count; ++i) {
            int startIndex = i * DANMU_MAX_SEND_LENGTH;
            messges[i] = sendMsg.substring(startIndex, Math.min(startIndex + DANMU_MAX_SEND_LENGTH, sendMsgLength));
        }

        return messges;
    }

    private int getNextCookiedIdx() throws InterruptedException {
        int result = curCookiedIdx;
        ++curCookiedIdx;
        if(curCookiedIdx >= cookieds.length){
            curCookiedIdx = 0;
        }

        if(result == 0){
            //要距离上一次使用的间隔大于1秒
            long diff = System.currentTimeMillis() - lastTimeOfUseCookied;
            if(diff < GET_COOKIED_INDEX_TIMEOUT){
                TimeUnit.MILLISECONDS.sleep(GET_COOKIED_INDEX_TIMEOUT - diff);
            }
            lastTimeOfUseCookied = System.currentTimeMillis();
        }

        return result;
    }

    private synchronized void startReadRoom(){
        if(danmuQues == null){
            danmuQues = new LinkedBlockingQueue<String>();
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
                    String content = danmuQues.poll(1, TimeUnit.SECONDS);
                    if(!readingDanmu){
                        break;
                    }

                    if(content == null){
                        continue;
                    }

                    if (!StringUtils.hasText(content)) {
                        continue;
                    }

                    Matcher matcher = targetDanmuPattern.matcher(content);
                    if (!matcher.find()) {
                        continue;
                    }

                    if(!isPauseDanmu()){
                        sendMsgWithTry(content, true);
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private synchronized void setPauseDanmu(boolean b){
        pauseDanmu = b;
    }

    private synchronized boolean isPauseDanmu(){
        return pauseDanmu;
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
        String[] messges = splitMessage(danmu.getContent());
        if(null == messges){
            return;
        }

        for (String messge : messges) {
            danmuQues.offer(messge);
        }
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
        JOptionPane.showMessageDialog(this, msg, "错误", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //加载设置文件的内容
        loadSettingInfo();
    }
}
