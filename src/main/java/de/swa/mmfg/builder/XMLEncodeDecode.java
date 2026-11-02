package de.swa.mmfg.builder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

import de.swa.mmfg.MMFG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** exports or imports a MMFG as XML or from XML **/
public class XMLEncodeDecode implements Flattener, Unflattener {
	private static final Logger log = LoggerFactory.getLogger(XMLEncodeDecode.class);

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
		XStream xstream = new XStream() {
			@Override
			protected MapperWrapper wrapMapper(MapperWrapper next) {
				return new IgnoredElementLogger(next);
			}
		};
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

	/** logs elements skipped by ignoreUnknownElements once per name and parsed MMFG **/
	private static class IgnoredElementLogger extends MapperWrapper {
		private final Set<String> logged = new HashSet<String>();

		IgnoredElementLogger(MapperWrapper next) {
			super(next);
		}

		@Override
		public boolean shouldSerializeMember(Class definedIn, String fieldName) {
			boolean known = super.shouldSerializeMember(definedIn, fieldName);
			// XStream asks for Object.class last, after no class in the hierarchy had the field
			if (!known && definedIn == Object.class && logged.add(fieldName)) {
				log.warn("Ignoring unknown element '{}' while parsing MMFG, its data is not loaded", fieldName);
			}
			return known;
		}
	}
}
