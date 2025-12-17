package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.Comparator;
import java.util.List;

import mgr.Factory;
import mgr.Manager;
import facade.DataEngineImpl;

public class ReviewManager extends DataEngineImpl<Review> {

    private static ReviewManager instance = new ReviewManager();

    private static final String[] headers = {"Review ID", "식당 이름", "메뉴 이름", "평점", "리뷰 내용", "작성자 ID", "작성일"};

    private ReviewManager() {
        setLabels(headers);
    }

    public static ReviewManager getInstance() {
        return instance;
    }

    @Override
    public void addNewRow(String[] uiTexts) {
        // Review 객체 생성 및 데이터 설정
        // ID는 Manager가 자동 생성하거나 (여기서는 임시로 timestamp 사용)
        String newId = "R" + System.currentTimeMillis();

        Review newReview = new Review();
        newReview.setReviewID(newId);

        // UIData의 set() 메서드를 활용하여 데이터 할당
        newReview.setCafeteriaName(uiTexts[1]);
        newReview.setMenuName(uiTexts[2]);
        // setRating()은 int를 받으므로 변환 필요
        try {
            newReview.setRating(Integer.parseInt(uiTexts[3]));
        } catch (Exception e) {
            newReview.setRating(1); // 오류 시 기본값
        }
        newReview.setContent(uiTexts[4]);
        newReview.setAuthorID(uiTexts[5]);
        newReview.setWrittenDate(new Date()); // 작성일은 현재 시간으로 설정

        //mList에 추가하고 파일 저장
        writeReview(newReview);
    }
    
    public void saveReviews(String fileName) {
        try (PrintWriter pw = new PrintWriter(fileName)) {
            // 부모 클래스의 mList를 사용합니다.
            for (Review r : mList) {
                String line = String.join("\t",
                        r.getReviewID(),
                        r.getCafeteriaName(),
                        r.getMenuName(),
                        r.getAuthorID(),
                        String.valueOf(r.getRating()),
                        // 주의: Review.read()에서 nextLine()으로 content를 맨 마지막에 읽으므로
                        // 저장할 때도 content를 반드시 맨 마지막에 둬야 합니다.
                        String.valueOf(r.getWrittenDate().getTime()),
                        String.valueOf(r.getWarningNum()),
                        r.getContent()
                );
                pw.println(line);
            }
        } catch (FileNotFoundException e) {
            System.out.println(fileName + " 파일 쓰기 오류: " + e.getMessage());
        }
    }

	public void writeReview(Review review) {
        mList.add(review);
        saveReviews("reviews.txt");
    }

    public ArrayList<Review> findReviewsByMenu(String cafeteriaName, String menuName) {
        ArrayList<Review> results = new ArrayList<>();
        for (Review r : mList) {
            if (r.matchesMenu(cafeteriaName, menuName)) {
                results.add(r);
            }
        }
        return results;
    }

    public ArrayList<Review> findReviewsByAuthor(String authorId) {
        ArrayList<Review> results = new ArrayList<>();
        for (Review r : mList) {
            if (r.matchesAuthor(authorId)) {
                results.add(r);
            }
        }
        return results;
    }

    // 주문 내역 등에서 리뷰 존재 여부 확인 (헬퍼 메서드 재사용)
    public boolean doesReviewExist(String authorId, String cafeteriaName, String menuName) {
        for (Review r : mList) {
            if (r.matchesAuthor(authorId) && r.matchesMenu(cafeteriaName, menuName)) {
                return true;
            }
        }
        return false;
    }

    public double getAverageRatingForMenu(String cafeteriaName, String menuName) {
        ArrayList<Review> menuReviews = findReviewsByMenu(cafeteriaName, menuName);

        if (menuReviews.isEmpty()) {
            return 0.0;
        }

        int totalRating = 0;
        for (Review r : menuReviews) {
            totalRating += r.getRating();
        }
        return (double) totalRating / menuReviews.size();
    }

    public void displayReviews() {
        ArrayList<Review> sortedList = new ArrayList<>(mList);

        sortedList.sort(new Comparator<Review>() {
            @Override
            public int compare(Review r1, Review r2) {
                return Integer.compare(r2.getRating(), r1.getRating());
            }
        });

        System.out.println("\n--- 전체 리뷰 목록 (평점순) --------------------------------------------");
        for (Review r : sortedList) {
            r.print();
        }
        System.out.println("-----------------------------------------------------------------------");
    }

    public boolean deleteReview(String reviewID) {
        Review reviewToRemove = find(reviewID);

        if (reviewToRemove != null) {
            mList.remove(reviewToRemove);
            saveReviews("reviews.txt");
            System.out.println("리뷰가 삭제되었습니다.");
            return true;
        } else {
            System.out.println("리뷰를 찾을 수 없습니다.");
            return false;
        }
    }

    public boolean reportReview(String reviewID) {
        Review reviewToReport = find(reviewID);

        if (reviewToReport != null) {
            saveReviews("reviews.txt");
            System.out.println("리뷰가 신고되었습니다. (현재 신고 획수: " + reviewToReport.getWarningNum() + "회)");
            return true;
        } else {
            System.out.println("신고하려는 리뷰를 찾을 수 없습니다.");
            return false;
        }
    }
    public ArrayList<Review> searchReviews(String kwd) {
        // 부모 클래스(Manager)가 제공하는 findAll 메서드를 사용하여 검색
        // Review.matches(kwd)가 호출됩니다.
        return (ArrayList<Review>) findAll(kwd);
    }

    /*신고 횟수가 threshold(10) 이상인 리뷰 목록을 반환*/
    public ArrayList<Review> getReportedReviews(int threshold) {
        ArrayList<Review> reportedList = new ArrayList<>();
        for (Review r : mList) {
            if (r.getWarningNum() >= threshold) {
                reportedList.add(r);
            }
        }
        //신고 횟수 내림차순
        reportedList.sort(new Comparator<Review>() {
            @Override
            public int compare(Review r1, Review r2) {
                return Integer.compare(r2.getWarningNum(), r1.getWarningNum());
            }
        });
        return reportedList;
    }

    //findReviewsByMenu, findReviewsByAuthor, getAverageRatingForMenu,displayReviews, searchReviews는 GUI에서 직접 사용하지 않거나,
    //     DataEngineImpl의 search(kwd)로 대체될 수 있음
    //    하지만 기존의 비즈니스 로직이므로 일단 유지
}
