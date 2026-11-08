package de.swa.gc;

import com.google.gson.Gson;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.google.gson.JsonSyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Vector;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GraphCodeIOTest {

	@Test
	public void asJson_andRead_roundTrip() {
		GraphCode gc = new GraphCode();
		gc.setDictionary(new java.util.Vector<String>() {{ add("term_a"); add("term_b"); }});
		gc.setValueForTerms("term_a", "term_b", 3);
		String json = GraphCodeIO.asJson(gc);
		GraphCode parsed = new Gson().fromJson(json, GraphCode.class);
		assertEquals(2, parsed.getDictionary().size());
		assertEquals(3, parsed.getEdgeValueForTerms("term_a", "term_b"));
	}

	@Test
	public void writeAndRead_fileRoundTrip() throws Exception {
		GraphCode gc = new GraphCode();
		gc.setDictionary(new java.util.Vector<String>() {{ add("term_x"); add("term_y"); }});
		gc.setValueForTerms("term_x", "term_y", 7);
		File temp = File.createTempFile("gc_test_", ".gc");
		temp.deleteOnExit();
		GraphCodeIO.write(gc, temp);
		assertTrue(temp.exists());
		GraphCode read = GraphCodeIO.read(temp);
		assertEquals(2, read.getDictionary().size());
		assertEquals(7, read.getEdgeValueForTerms("term_x", "term_y"));
	}

	@Test
	public void flatten_generatesJsonForMMFG() {
		MMFG m = new MMFG();
		new Node("Root-Asset", m);
		new Node("car", m);
		String json = new GraphCodeIO().flatten(m);
		assertNotNull(json);
		assertTrue(json.contains("car"));
	}

	private static GraphCode graphCode(String... terms) {
		GraphCode gc = new GraphCode();
		gc.setDictionary(new Vector<>(Arrays.asList(terms)));
		return gc;
	}

	private static File tempFile() throws Exception {
		File temp = File.createTempFile("gc_test_", ".gc");
		temp.deleteOnExit();
		return temp;
	}

	@Test
	public void read_missingFile_returnsNull() {
		assertNull(GraphCodeIO.read(new File("does-not-exist.gc")));
	}

	@Test
	public void read_corruptFile_throws() throws Exception {
		File temp = tempFile();
		Files.write(temp.toPath(), "{\"dictionary\":[".getBytes());

		assertThrows(JsonSyntaxException.class, () -> GraphCodeIO.read(temp));
	}

	@Test
	public void write_replacesLongerExistingContent() throws Exception {
		File temp = tempFile();
		GraphCodeIO.write(graphCode("a_very_long_term", "another_very_long_term", "and_one_more"), temp);
		GraphCodeIO.write(graphCode("term_a", "term_b"), temp);

		assertEquals(2, GraphCodeIO.read(temp).getDictionary().size());
	}

	@Test
	public void writeAndRead_keepsCollectionElements() throws Exception {
		File temp = tempFile();
		GraphCode root = graphCode("term_a", "term_b");
		root.addGraphCode(graphCode("term_c", "term_d"));
		GraphCodeIO.write(root, temp);

		GraphCode read = GraphCodeIO.read(temp);
		assertTrue(read.isCollection());
		assertTrue(read.getCollectionElements().get(0).getDictionary().contains("term_c"));
	}

	@Test
	public void writeAndRead_keepsLatin1Terms() throws Exception {
		File temp = tempFile();
		GraphCodeIO.write(graphCode("straße", "größe"), temp);

		assertEquals(graphCode("straße", "größe").getDictionary(), GraphCodeIO.read(temp).getDictionary());
	}

	@Test
	public void writeAndRead_keepsTermsOutsideLatin1() throws Exception {
		File temp = tempFile();
		GraphCodeIO.write(graphCode("日本語", "русский"), temp);

		assertEquals(graphCode("日本語", "русский").getDictionary(), GraphCodeIO.read(temp).getDictionary());
	}

	@Test
	public void fileFormat_isJsonWithoutHeaderOrFooter() {
		GraphCodeIO io = new GraphCodeIO();
		assertEquals("json", io.getFileExtension());
		assertEquals("", io.startFile());
		assertEquals("", io.endFile());
	}
}
