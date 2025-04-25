module com.apitester {
	requires javafx.controls;
	requires javafx.base;
	requires javafx.graphics;
	requires org.json;
	requires java.xml;
	requires java.net.http;

	opens com.apitester to javafx.graphics;
}