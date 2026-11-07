package de.swa.importers;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import com.google.gson.Gson;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** class to import RSS feeds **/
public class RSSImporter { 
	private static final Logger log = LoggerFactory.getLogger(RSSImporter.class);

	public RSSImporter(URL url, String collectionFolder) {
		try {
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(url));

			for (SyndEntry item : feed.getEntries()) {
				String title = item.getTitle();
				String uriId = item.getUri();
				String descr = item.getDescription().getValue();
				
				URI uri = URI.create(uriId);
				log.info("LINK : {}", uri);
				log.info("URI  : {}", uriId);
				log.info("TITLE: {}", title);
				log.info("DESCR: {}", descr);
				HttpRequest request = HttpRequest.newBuilder(uri).build();
				String content = HttpClient.newHttpClient().send(request, BodyHandlers.ofString()).body();
				
				StringBuilder sb = new StringBuilder();
				HTMLEditorKit.ParserCallback parserCallback = new HTMLEditorKit.ParserCallback() {
				    public boolean readyForNewline;

				    @Override public void handleText(final char[] data, final int pos) {
				        String s = new String(data);
				        sb.append(s.trim());
				        readyForNewline = true;
				    }
				    @Override public void handleStartTag(final HTML.Tag t, final MutableAttributeSet a, final int pos) {
				        if (readyForNewline && (t == HTML.Tag.DIV || t == HTML.Tag.BR || t == HTML.Tag.P || t == HTML.Tag.LI)) {
				            sb.append("\n");
				            readyForNewline = false;
				        }
				    }
				    @Override public void handleSimpleTag(final HTML.Tag t, final MutableAttributeSet a, final int pos) {
				        handleStartTag(t, a, pos);
				    }
				};
				new ParserDelegator().parse(new StringReader(content), parserCallback, false);
				String plainText = sb.toString();			
				if (plainText.indexOf("Site Index") > 0) plainText = plainText.substring(0, plainText.indexOf("Site Index"));
				if (plainText.indexOf("Sections") > 0) plainText = plainText.substring(plainText.indexOf("Sections"), plainText.length());
				log.info("TXT: {}", plainText);
				
				RssItem ri = new RssItem();
				ri.setTitle(title);
				ri.setLink(uriId);
				ri.setDescription(descr);
				ri.setContent(plainText);
				ri.setAuthor(item.getAuthor());
				ri.setDate(item.getPublishedDate().toGMTString());

				uriId = uriId.replace("/", "_");
				File f = new File(collectionFolder + File.separatorChar + uriId + ".json");
				if (!f.exists()) {
					Gson gson = new Gson();
					String json = gson.toJson(ri);
					RandomAccessFile rf = new RandomAccessFile(f, "rw");
					rf.setLength(0);
					rf.writeBytes(json);
					rf.close();
				}
			}
		}
		catch (Exception x) {
			x.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		new RSSImporter(new URL("https://rss.nytimes.com/services/xml/rss/nyt/World.xml"), "rss");
	}
}
