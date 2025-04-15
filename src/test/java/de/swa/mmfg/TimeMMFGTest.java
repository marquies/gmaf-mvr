package de.swa.mmfg;

import de.swa.gc.TimeGraphCode;
import de.swa.gc.TimeGraphCodeGenerator;
import junit.framework.Assert;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Vector;

/**
 * Created by Patrick Steinert on 14.04.25.
 */
public class TimeMMFGTest {

	/**
	 * tests the basic functionality of TimeMMFG
	 *
	 * adding one feature with a time range of 0-10 should result in a TimeMMFG
	 * with a time range of 0-10
	 */
	@Test
	public void testTimeMMFG() {
		MMFG timeMMFG = new MMFG();
		Node n1 = new Node("a", timeMMFG);
		Node n2 = new Node("b", timeMMFG);


		timeMMFG.addNode(n1);
		timeMMFG.addNode(n2);

		CompositionRelationship cr = new CompositionRelationship(CompositionRelationship.RELATION_PART_OF, n2);
		//CompositionRelationship cr2 = new CompositionRelationship(CompositionRelationship.RELATION_PART_OF, n2);

		Vector<Timerange> timeranges = new Vector<>();
		timeranges.add(new Timerange(new Date(0), new Date(10)));
		cr.setTimeRange(timeranges);

		n1.addCompositionRelationship(cr);

		Assert.assertFalse(timeMMFG.getNodes().isEmpty());
		Node element = timeMMFG.getNodes().get(0);
		Assert.assertEquals("a", element.getName()); //timeMMFG.getNodes().get(0).getName()

		Vector<CompositionRelationship> elementCRs = element.getCompositionRelationships();
		Assert.assertFalse(elementCRs.isEmpty());


		//timeMMFG.addCompositionRelationship(cr2);

	}

	@Test
	public void testTimeMMFG2GraphCodeSimple() {
		MMFG timeMMFG = new MMFG();
		Node n1 = new Node("alpha", timeMMFG);
		Node n2 = new Node("beta", timeMMFG);


		n1.setTimerange(new Timerange(new Date(2*1000), new Date(10*1000)));
		n2.setTimerange(new Timerange(new Date(1*1000), new Date(8*1000)));

		timeMMFG.addNode(n1);
		timeMMFG.addNode(n2);

		CompositionRelationship cr = new CompositionRelationship(CompositionRelationship.RELATION_PART_OF, n2);
		//CompositionRelationship cr2 = new CompositionRelationship(CompositionRelationship.RELATION_PART_OF, n2);

		Vector<Timerange> timeranges = new Vector<>();
		timeranges.add(new Timerange(new Date(0*1000), new Date(10*1000)));
		cr.setTimeRange(timeranges);

		n1.addCompositionRelationship(cr);

		TimeGraphCode tgc = TimeGraphCodeGenerator.generate(timeMMFG);

		Vector<String> dict = tgc.getDictionary();
		Assert.assertFalse(dict.isEmpty());
		Assert.assertEquals(2, tgc.getDictionary().size());

		Assert.assertEquals(16, tgc.getEdgeValueForTerms("alpha", "beta", 5));

		Timerange min = TimeGraphCodeGenerator.findMinTimeRange(timeMMFG);
		Timerange max = TimeGraphCodeGenerator.findMaxTimeRange(timeMMFG);


		Assert.assertEquals(10000, (max.getEnd().getTime()-min.getBegin().getTime()));

	}


	@Test
	public void testTimeMMFG2GraphCodeComplex() {
		MMFG timeMMFG = new MMFG();
		Node n1 = new Node("alpha", timeMMFG);
		Node n2 = new Node("beta", timeMMFG);


		n1.setTimerange(new Timerange(new Date(2*1000), new Date(13*1000)));
		n2.setTimerange(new Timerange(new Date(1*1000), new Date(8*1000)));

		timeMMFG.addNode(n1);
		timeMMFG.addNode(n2);

		CompositionRelationship cr = new CompositionRelationship(CompositionRelationship.RELATION_PART_OF, n2);
		//CompositionRelationship cr2 = new CompositionRelationship(CompositionRelationship.RELATION_PART_OF, n2);

		Vector<Timerange> timeranges = new Vector<>();
		timeranges.add(new Timerange(new Date(0*1000), new Date(10*1000)));
		cr.setTimeRange(timeranges);

		n1.addCompositionRelationship(cr);

		TimeGraphCode tgc = TimeGraphCodeGenerator.generate(timeMMFG);

		Vector<String> dict = tgc.getDictionary();
		Assert.assertFalse(dict.isEmpty());
		Assert.assertEquals(2, tgc.getDictionary().size());
		Assert.assertEquals(16, tgc.getEdgeValueForTerms("alpha", "beta", 5));

		Timerange min = TimeGraphCodeGenerator.findMinTimeRange(timeMMFG);
		Timerange max = TimeGraphCodeGenerator.findMaxTimeRange(timeMMFG);

		Assert.assertEquals(13*1000, (max.getEnd().getTime()-min.getBegin().getTime()));

		

	}

}
