package de.swa.gc;

import java.io.File;
import java.io.RandomAccessFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Vector;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.builder.Flattener;

/** Utility Class for Graph Code Import and Export, implements the MMFG-Flattener-Interface
 * 
 * @author stefan_wagenpfeil
 */

public class GraphCodeIO implements Flattener {
	/** exports a Graph Code based on a MMFG to Json **/
	public String flatten(MMFG fv) {
		GraphCode gc = GraphCodeGenerator.generate(fv);
		return asJson(gc);
	}

	/** returns "json" **/
	public String getFileExtension() {
		return "json";
	}

	/** reads a Graph Code from a Json-File **/
	public static GraphCode read(File f) {
		String s = "";
		try {
			RandomAccessFile rf = new RandomAccessFile(f, "r");
			byte[] b = new byte[(int)rf.length()];
			rf.read(b);
			s = new String(b);
			rf.close();
		} catch (Exception x) {
			x.printStackTrace();
		}
		
		// Create a custom Gson instance that can handle TimeGraphCode
		Gson gson = createCustomGson();
		
		// Try to deserialize as TimeGraphCode first
		try {
			if (s.contains("\"intervalLength\"")) {
				TimeGraphCode tgc = gson.fromJson(s, TimeGraphCode.class);
				return tgc;
			}
		} catch (Exception x) {
			System.out.println("Failed to deserialize as TimeGraphCode, falling back to GraphCode");
		}
		
		// Fall back to regular GraphCode
		GraphCode gc = gson.fromJson(s, GraphCode.class);
		return gc;
	}
	
	/** writes a Graph Code as Json into File f **/
	public static void write(GraphCode gc, File f) {
		// Create a custom Gson instance that can handle TimeGraphCode
		Gson gson = createCustomGson();
		
		String s = gson.toJson(gc);
		try {
			RandomAccessFile rf = new RandomAccessFile(f, "rw");
			rf.setLength(0);
			rf.writeBytes(s);
			rf.close();
		} catch (Exception x) {
			x.printStackTrace();
		}
	}
	
	/** returns a Graph Code as JSon **/
	public static String asJson(GraphCode gc) {
		// Create a custom Gson instance that can handle TimeGraphCode
		Gson gson = createCustomGson();
		return gson.toJson(gc);
	}

	/** for Graph Codes, no header is required **/
	public String startFile() {
		return "";
	}
	
	/** for Graph Codes, no footer is required **/
	public String endFile() {
		return "";
	}
	
	/**
	 * Creates a custom Gson instance with type adapters for TimeGraphCode
	 */
	private static Gson createCustomGson() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		
		// Register type adapter for TimeGraphCode
		gsonBuilder.registerTypeAdapter(TimeGraphCode.class, new TimeGraphCodeTypeAdapter());
		
		return gsonBuilder.create();
	}
	
	/**
	 * Custom type adapter for TimeGraphCode to handle the matrix field name conflict
	 */
	private static class TimeGraphCodeTypeAdapter implements JsonSerializer<TimeGraphCode>, JsonDeserializer<TimeGraphCode> {
		
		@Override
		public JsonElement serialize(TimeGraphCode src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jsonObject = new JsonObject();
			
			// Add class type identifier
			jsonObject.addProperty("@type", "TimeGraphCode");
			
			// Serialize dictionary
			jsonObject.add("dictionary", context.serialize(src.getDictionary()));
			
			// Serialize collection elements
			jsonObject.add("collectionElements", context.serialize(src.getCollectionElements()));
			
			// Serialize the matrix - for TimeGraphCode, this is the 3D matrix
			if (src.matrix != null) {
				jsonObject.add("matrix", context.serialize(src.matrix));
			}
			
			// Serialize interval length
			jsonObject.addProperty("intervalLength", src.getIntervalLength());
			
			return jsonObject;
		}
		
		@Override
		public TimeGraphCode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jsonObject = json.getAsJsonObject();
			
			// Create a new TimeGraphCode
			TimeGraphCode tgc = new TimeGraphCode(
				jsonObject.has("intervalLength") ? jsonObject.get("intervalLength").getAsInt() : 0
			);
			
			// Deserialize dictionary
			if (jsonObject.has("dictionary")) {
				tgc.setDictionary(context.deserialize(jsonObject.get("dictionary"), Vector.class));
			}
			
			// We're skipping collection elements as commented out in the previous edit
			
			// Deserialize the matrix - for TimeGraphCode, this is the 3D matrix
			if (jsonObject.has("matrix")) {
				// Check if we have a type field to determine the matrix type
				if (jsonObject.has("@type") && "TimeGraphCode".equals(jsonObject.get("@type").getAsString())) {
					// This is a 3D matrix for TimeGraphCode
					tgc.matrix = context.deserialize(jsonObject.get("matrix"), int[][][].class);
				} else {
					// This is a 2D matrix for GraphCode - convert to 3D with a single time point
					int[][] matrix2D = context.deserialize(jsonObject.get("matrix"), int[][].class);
					if (matrix2D != null) {
						tgc.matrix = new int[1][][];
						tgc.matrix[0] = matrix2D;
					}
				}
			}
			
			return tgc;
		}
	}
}
