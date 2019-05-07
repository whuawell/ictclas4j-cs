package nlp.cws.tag;

public enum TAG_TYPE {
	TT_NORMAL, TT_PERSON, TT_PLACE, TT_TRANS_PERSON;

	public int getValue() {
		return this.ordinal();
	}

	public static TAG_TYPE forValue(int value) {
		return values()[value];
	}
}