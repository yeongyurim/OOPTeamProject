package gui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

public class ReviewWriteUI extends JFrame {
	
    // 호출부에서 주입 (다른 UI에서 선택된 식당/메뉴/로그인 사용자)
    private final String cafeteriaName;
    private final String menuName;
    private final String authorId;

    // UI
    private JTextArea reviewArea;
    private ButtonGroup ratingGroup;
    private int selectedRating = 0;

    public ReviewWriteUI(String cafeteriaName, String menuName, String authorId) {
        this.cafeteriaName = cafeteriaName;
        this.menuName = menuName;
        this.authorId = authorId;
        initUI();
    }

    private void initUI() {
        setTitle("리뷰 작성");
        setSize(900, 700);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        Container cp = getContentPane();
        cp.setLayout(null);
        cp.setBackground(Color.WHITE);

        // 상단 타이틀
        JLabel title = new JLabel("리뷰 작성", SwingConstants.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 22));
        title.setBounds(0, 20, 900, 28);
        cp.add(title);

        JSeparator sep = new JSeparator();
        sep.setBounds(60, 60, 780, 1);
        cp.add(sep);

        // 메뉴 헤더(이미지+설명) — 기존 리뷰 목록 UI 스타일 재사용
        JPanel header = new JPanel(null);
        header.setBackground(Color.WHITE);
        header.setBorder(new LineBorder(new Color(220,220,220)));
        header.setBounds(100, 80, 700, 150);
        cp.add(header);

        JLabel mn = new JLabel(menuName);
        mn.setFont(new Font("Dialog", Font.BOLD, 18));
        mn.setBounds(160, 18, 480, 22);
        header.add(mn);

        JLabel cafe = new JLabel(cafeteriaName);
        cafe.setFont(new Font("Dialog", Font.PLAIN, 14));
        cafe.setForeground(new Color(100,100,100));
        cafe.setBounds(160, 44, 480, 18);
        header.add(cafe);

        // 메뉴 정보 로드 (menus.txt: [0]=카페, [1]=메뉴, [3]=설명, [5]=이미지)
        String imgName = null, desc = null;
        String[] info = findMenuInfo(cafeteriaName, menuName);
        if (info != null) { imgName = info[0]; desc = info[1]; }

        JLabel img = new JLabel();
        img.setOpaque(true);
        img.setBackground(new Color(240,240,240));
        img.setBounds(20, 20, 120, 110);
        ImageIcon icon = loadScaledIcon(imgName, 120, 110);
        if (icon != null) img.setIcon(icon);
        header.add(img);

        JLabel descLb = new JLabel("<html>"+(desc==null?"":desc)+"</html>");
        descLb.setFont(new Font("Dialog", Font.PLAIN, 13));
        descLb.setBounds(160, 66, 510, 60);
        header.add(descLb);

        // 리뷰 입력 패널
        JPanel write = new JPanel(null);
        write.setBackground(Color.WHITE);
        write.setBorder(new LineBorder(new Color(220,220,220)));
        write.setBounds(100, 250, 700, 320);
        cp.add(write);

        // 별점 선택(1~5) — 기존 카드의 별 표현과 호환
        JLabel starTitle = new JLabel("별점 선택");
        starTitle.setFont(new Font("Dialog", Font.BOLD, 14));
        starTitle.setBounds(20, 16, 120, 20);
        write.add(starTitle);

        ratingGroup = new ButtonGroup();
        int x = 110;
        for (int r=1; r<=5; r++) {
            final int rating = r;
            JToggleButton btn = new JToggleButton("★"+r);
            btn.setFont(new Font("Dialog", Font.PLAIN, 13));
            btn.setBackground(new Color(255, 255, 255));
            btn.setBounds(x, 12, 55, 28);
            btn.addActionListener(e -> selectedRating = rating);
            ratingGroup.add(btn);
            write.add(btn);
            x += 60;
        }

        // 작성자/날짜 가이드 (보기용)
        JLabel authorLb = new JLabel("작성자: " + authorId);
        authorLb.setFont(new Font("Dialog", Font.PLAIN, 12));
        authorLb.setForeground(Color.GRAY);
        authorLb.setBounds(20, 46, 240, 16);
        write.add(authorLb);

        JLabel dateLb = new JLabel("작성일: " + new SimpleDateFormat("yyyy.MM.dd").format(new java.util.Date()));
        dateLb.setFont(new Font("Dialog", Font.PLAIN, 12));
        dateLb.setForeground(new Color(140,140,140));
        dateLb.setBounds(20, 62, 240, 16);
        write.add(dateLb);

        // 리뷰 내용 입력
        reviewArea = new JTextArea();
        reviewArea.setFont(new Font("Dialog", Font.PLAIN, 14));
        reviewArea.setLineWrap(true);
        reviewArea.setWrapStyleWord(true);
        reviewArea.setBorder(new LineBorder(new Color(210,210,210)));

        JScrollPane scroll = new JScrollPane(reviewArea);
        scroll.setBounds(20, 86, 660, 180);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        write.add(scroll);

        // 버튼들
        JButton submit = new JButton("작성 완료");
        submit.setBounds(540, 274, 140, 28);
        submit.setBackground(new Color(255, 255, 255));
        submit.addActionListener(e -> onSubmit());
        write.add(submit);

        JButton cancel = new JButton("취소");
        cancel.setBounds(440, 274, 90, 28);
        cancel.setBackground(new Color(255, 255, 255));
        cancel.addActionListener(e -> dispose());
        write.add(cancel);
    }

    private void onSubmit() {
        String content = sanitize(reviewArea.getText());
        if (selectedRating < 1 || selectedRating > 5) {
            JOptionPane.showMessageDialog(this, "별점을 선택해 주세요 (1~5).");
            return;
        }
        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "리뷰 내용을 입력해 주세요.");
            return;
        }

        // reviews.txt 포맷: id, cafe, menu, author, rating, writtenMs, warning(초기0), content
        long now = System.currentTimeMillis();
        String reviewId = "R"+ now;  // 기존 포맷과 동일 패턴
        int warning = 0;

        String line = reviewId + "\t" +
                cafeteriaName + "\t" +
                menuName + "\t" +
                authorId + "\t" +
                selectedRating + "\t" +
                now + "\t" +
                warning + "\t" +
                content;

        File f = new File("reviews.txt");
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(f, true), StandardCharsets.UTF_8))) {
            bw.write(line);
            bw.newLine();
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "저장 오류: " + ex.getMessage());
            return;
        }

        JOptionPane.showMessageDialog(this, "리뷰가 저장되었습니다.");
        // 초기화 & 닫기(또는 호출부가 목록 새로고침)
        reviewArea.setText("");
        ratingGroup.clearSelection();
        selectedRating = 0;
        dispose();
    }

    // =============== 기존 리뷰 목록 UI 스타일과 동일한 유틸 ===============

    // menus.txt: [0]=카페, [1]=메뉴, [3]=설명, [5]=이미지
    private String[] findMenuInfo(String cafeName, String mName) {
        File file = new File("menus.txt");
        if (!file.exists()) return null;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\t", -1);
                if (p.length < 6) continue;
                if (p[0].trim().equals(cafeName) && p[1].trim().equals(mName)) {
                    return new String[]{ p[5].trim(), p[3].trim() };
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private ImageIcon loadScaledIcon(String fileName, int w, int h) {
        if (fileName == null || fileName.isEmpty()) return null;
        File f = new File("images/" + fileName);
        ImageIcon origin = null;
        if (f.exists()) origin = new ImageIcon(f.getAbsolutePath());
        else {
            java.net.URL url = getClass().getResource("/images/" + fileName);
            if (url != null) origin = new ImageIcon(url);
        }
        if (origin == null) return null;
        Image scaled = origin.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        // 탭/개행을 공백으로 치환해 파일 포맷 깨짐 방지
        return s.replace('\t',' ').replace('\r',' ').replace('\n',' ').trim();
    }
}
