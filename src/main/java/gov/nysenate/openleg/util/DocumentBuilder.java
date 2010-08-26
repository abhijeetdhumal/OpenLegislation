package gov.nysenate.openleg.util;

import gov.nysenate.openleg.PMF;
import gov.nysenate.openleg.lucene.LuceneField;
import gov.nysenate.openleg.model.Bill;
import gov.nysenate.openleg.model.Transcript;
import gov.nysenate.openleg.model.calendar.Calendar;
import gov.nysenate.openleg.model.committee.Meeting;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


public class DocumentBuilder {
	
	public static final String JAVA_PRIMITIVES = "char|byte|short|int|long|float|double|boolean|void|String";
	public static final String GET = "get";
	public static final String LUCENE = "Lucene";
	
	public static final org.apache.lucene.document.Field.Store DEFAULT_STORE = org.apache.lucene.document.Field.Store.YES;
	public static final org.apache.lucene.document.Field.Index DEFAULT_INDEX = org.apache.lucene.document.Field.Index.ANALYZED;
	
	public static final String LUCENE_OTYPE = "luceneOtype";
	public static final String LUCENE_OID = "luceneOid";
	public static final String LUCENE_OSEARCH = "luceneOsearch";
	public static final String LUCENE_FIELDS = "luceneFields";
	
	public static final String OTYPE = "otype";
	public static final String OID = "oid";
	public static final String OSEARCH = "osearch";
	
	public static void main(String[] args) throws Exception {
						
//		
//		DocumentBuilder db = new DocumentBuilder();
//		TestObject to = db.new TestObject();
//		
//		converter(to, null);		
		
//		/*bill from db*/
//		Bill b = PMF.getDetachedBill("S5000");
//		System.out.println(b.getSponsor().getFullname());
//		HashMap<String,org.apache.lucene.document.Field> map = new DocumentBuilder().converter(b, null);
//		
//		for(String s:map.keySet()) {
//			org.apache.lucene.document.Field field = map.get(s);
//			System.out.println(s);
//		}
		
		for(Calendar cal:(List<Calendar>)PMF.getDetachedObjects(Calendar.class, null, 0, 100)) {
			System.out.println(cal.getType());
		}
		
		
		/*calendar from db*/
		System.out.println("\n\n-----CALENDAR-----\n\n");
		Calendar c = (Calendar)PMF.getDetachedObject(Calendar.class, "id", "cal-active-00060-2009", "no descending");	
		System.out.println(c.getType());
//		HashMap<String,org.apache.lucene.document.Field> map = new DocumentBuilder().converter(c.getSupplementals().iterator().next(), null);
//		for(String s:map.keySet()) {
//			org.apache.lucene.document.Field field = map.get(s);
//			System.out.println(s);
//		}
//		
//		/*transcript from db*/
//		System.out.println("\n\n\n-----TRANSCRIPT-----\n\n");
//		Transcript t = PMF.getDetachedTranscript("292");
//		map = new DocumentBuilder().converter(t, null);
//		for(String s:map.keySet()) {
//			org.apache.lucene.document.Field field = map.get(s);
//			System.out.println(s);
//		}
//		
//		/*meeting from db*/
//		System.out.println("\n\n\n-----MEETING-----\n\n");
//		Collection<Meeting> meetings = PMF.getDetachedObjects(Meeting.class, "committeeName", ".*" + "Aging" + ".*", "meetingDateTime descending", 0, 1);
//		List<String> meeting_exclude = new ArrayList<String>();
//		meeting_exclude.add("votes");
//		for(Meeting m:meetings) {
//			map = new DocumentBuilder().converter(m, null);
//			for(String s:map.keySet()) {
//				org.apache.lucene.document.Field field = map.get(s);
//				System.out.println(s);
//			}
//		}
	}
	
	private String getLuceneFields(Object o, String method) throws Exception {
		Method m = o.getClass().getDeclaredMethod(method);
		return (String)m.invoke(o);		
	}
	
	private HashMap<String,org.apache.lucene.document.Field> converter(Object o, HashMap<String,org.apache.lucene.document.Field> fields) {		
		if(fields == null) {
			fields = new HashMap<String,org.apache.lucene.document.Field>();
		}
		
		try {
			fields.put(OTYPE,
					new org.apache.lucene.document.Field(
						OTYPE,
						getLuceneFields(o, LUCENE_OTYPE),
						DEFAULT_STORE,
						DEFAULT_INDEX));
			fields.put(OID,
					new org.apache.lucene.document.Field(
							OID,
						getLuceneFields(o, LUCENE_OID),
						DEFAULT_STORE,
						DEFAULT_INDEX));
			fields.put(OSEARCH,
					new org.apache.lucene.document.Field(
							OSEARCH,
						getLuceneFields(o, LUCENE_OSEARCH),
						DEFAULT_STORE,
						DEFAULT_INDEX));
			
			
			Field[] objectFields = o.getClass().getDeclaredFields();

			for(Field f:objectFields) {
				AnnotatedField af = null;
				if((af = getAnnotatedField(f)) != null) {
					String name = af.name.equals("!-_-!") ? f.getName().toLowerCase() : af.name;
					org.apache.lucene.document.Field.Store store = af.store;
					org.apache.lucene.document.Field.Index index = af.index;
					
					String fieldName = fixCase(f.getName());
					
					Method fieldMethod = null;
					Object fieldObject = null;
					try {
						fieldMethod = o.getClass().getDeclaredMethod(GET + LUCENE + fieldName);
						fieldObject = fieldMethod.invoke(o);
					}
					catch(NoSuchMethodException e1) {
						try {
							fieldMethod = o.getClass().getDeclaredMethod(GET + fieldName);
							fieldObject = fieldMethod.invoke(o);
						}
						catch (NoSuchMethodException e2) {
							f.setAccessible(true);
							fieldObject = f.get(o);
							f.setAccessible(false);
						}
					}
					
					if(fieldObject != null) {
						fields.put(name,
							new org.apache.lucene.document.Field(
								name,
								fieldObject.toString(),
								store,
								index));
					}
					else {
						fields.put(name,
								new org.apache.lucene.document.Field(
									name,
									"",
									store,
									index));
					}
				}
			}
			
			Method otherMethod = o.getClass().getDeclaredMethod(LUCENE_FIELDS);
			HashMap<String,org.apache.lucene.document.Field> otherMap = (HashMap<String,org.apache.lucene.document.Field>)otherMethod.invoke(o);
		
			if(otherMap != null) {
				fields.putAll(otherMap);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return fields;
	}
	
	public AnnotatedField getAnnotatedField(Field field) {
		LuceneField lf = field.getAnnotation(LuceneField.class);
		if(lf != null) {
			return new AnnotatedField(lf);
			
		}
		return null;		
	}
	
	public String classCompare(Class<?>[] list, Class<?> clazz) {
		for(Class<?> c:list) {
			if(c.equals(clazz)) {
				return clazz.getSimpleName();
				
			}
		}
		return null;
	}
	
	private String fixCase(String s) {
		char[] chars = s.toCharArray();
		
		chars[0] = Character.toUpperCase(chars[0]);
		
		return new String(chars);
	}	
	
	class AnnotatedField {
		public String name;
		public org.apache.lucene.document.Field.Store store;
		public org.apache.lucene.document.Field.Index index;
		
		public AnnotatedField(LuceneField luceneField) {
			name = luceneField.value();
			store = luceneField.store();
			index = luceneField.index();
		}	
	}
}