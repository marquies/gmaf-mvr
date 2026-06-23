package de.swa.gc.processing;

import de.swa.gc.GraphCode;
import org.junit.jupiter.api.Test;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DefaultCollectionProcessorTest {

	@Test
	public void getResultList_sortsBySimilarity_descending() {
		DefaultCollectionProcessor processor = new DefaultCollectionProcessor();

		GraphCode query = new GraphCode();
		query.setDictionary(new Vector<String>() {{ add("a"); }});
		query.setValueForTerms("a", "a", 1);
		processor.setQueryObject(query);

		GraphCode candidate1 = new GraphCode();
		candidate1.setDictionary(new Vector<String>() {{ add("a"); }});
		candidate1.setValueForTerms("a", "a", 1);
		GraphCodeMeta meta1 = new GraphCodeMeta("candidate1", candidate1);

		GraphCode candidate2 = new GraphCode();
		candidate2.setDictionary(new Vector<String>() {{ add("a"); add("b"); }});
		candidate2.setValueForTerms("a", "a", 1);
		candidate2.setValueForTerms("b", "b", 1);
		GraphCodeMeta meta2 = new GraphCodeMeta("candidate2", candidate2);

		Vector<GraphCodeMeta> metas = new Vector<>();
		metas.add(meta1);
		metas.add(meta2);

		processor.setOperation(CollectionProcessor.SIMILARITY);
		processor.preloadIndex(metas);
		processor.execute();
		Vector<GraphCodeMeta> result = processor.getResultList();

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("candidate1", result.get(0).getFileName());
		assertEquals("candidate2", result.get(1).getFileName());
	}
}
