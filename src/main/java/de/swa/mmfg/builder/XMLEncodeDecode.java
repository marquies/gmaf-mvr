package de.swa.mmfg.builder;

import java.util.Collection;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

import de.swa.mmfg.MMFG;

/** exports or imports a MMFG as XML or from XML **/
public class XMLEncodeDecode implements Flattener, Unflattener {
	public String flatten(MMFG fv) {	
		XStream xstream = new XStream();
		xstream.addPermission(NoTypePermission.NONE);
		xstream.addPermission(NullPermission.NULL);
		xstream.addPermission(PrimitiveTypePermission.PRIMITIVES);
		xstream.allowTypeHierarchy(Collection.class);
		xstream.allowTypesByWildcard(new String[] {
		    "de.swa.mmfg.**"
		});
		return xstream.toXML(fv);

//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		XMLEncoder enc = new XMLEncoder(baos);
//		enc.writeObject(fv);
//		enc.close();
//		return baos.toString();
	}
	
	public MMFG unflatten(String xml) {
		XStream xstream = new XStream();
		xstream.addPermission(NoTypePermission.NONE);
		xstream.addPermission(NullPermission.NULL);
		xstream.addPermission(PrimitiveTypePermission.PRIMITIVES);
		xstream.allowTypeHierarchy(Collection.class);
		xstream.allowTypesByWildcard(new String[] {
		    "de.swa.mmfg.**"
		});
		xstream.ignoreUnknownElements();

		return (MMFG)xstream.fromXML(xml);

//		XMLDecoder dec = new XMLDecoder(new ByteArrayInputStream(xml.getBytes()));
//		MMFG fv = (MMFG)dec.readObject();
//		return fv;
	}
	
	public String getFileExtension() {
		return "xml";
	}
	public String endFile() { return ""; }
	public String startFile() { return ""; }

}
