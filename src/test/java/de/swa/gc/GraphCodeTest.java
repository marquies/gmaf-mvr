package de.swa.gc;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Vector;

/**
 * Created by Patrick Steinert on 12.07.24.
 */
public class GraphCodeTest {

	private GraphCode graphCode;

	@BeforeEach
	public void setUp() {
		graphCode = new GraphCode();
	}

	@Test
	public void toString_returnsValidJsonRepresentation() {
		graphCode.setDictionary(new Vector<>(Arrays.asList("term1", "term2")));
		graphCode.setValueForTerms("term1", "term2", 1);
		String json = graphCode.toString();
		Gson gson = new Gson();
		GraphCode result = gson.fromJson(json, GraphCode.class);
		assertEquals(graphCode.getDictionary(), result.getDictionary());
		assertEquals(graphCode.getEdgeValueForTerms("term1", "term2"), result.getEdgeValueForTerms("term1", "term2"));
	}

	@Test
	public void toString_withEmptyGraphCode_returnsEmptyJsonRepresentation() {
		String json = graphCode.toString();
		assertEquals("{\"dictionary\":[],\"collectionElements\":[]}", json);
	}

	@Test
	public void toString_withCollectionElements_includesThemInJson() {
		GraphCode childGraphCode = new GraphCode();
		childGraphCode.setDictionary(new Vector<>(Arrays.asList("childTerm")));
		graphCode.addGraphCode(childGraphCode);
		String json = graphCode.toString();
		assertTrue(json.contains("collectionElements"));
		assertTrue(json.contains("childterm"));
	}
}