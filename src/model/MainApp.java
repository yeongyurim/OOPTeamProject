package model;

import mgr.Factory;

public class MainApp {
	private static MainApp app = null;
	
	private MainApp() {
		
	}
	
	public static MainApp getInstance() {
		if (app == null)
			app = new MainApp();
		return app;
	}
	
	static CafeteriaManager cafeteriaManager = CafeteriaManager.getInstance();
	static UserManager userManager = UserManager.getInstance();
	static ReviewManager reviewMgr =  ReviewManager.getInstance();
	
	public void run() {
		
		userManager.readAll("users.txt",User::new);
		userManager.printAll();
		reviewMgr.readAll("reviews.txt",Review::new);
		reviewMgr.displayReviews();
		cafeteriaManager.readAll("cafeterias.txt",Cafeteria::new);
		cafeteriaManager.printAll();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MainApp app = new MainApp();
		app.run();
	}
}
