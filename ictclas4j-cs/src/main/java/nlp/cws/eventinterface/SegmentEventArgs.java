package nlp.cws.eventinterface;

public class SegmentEventArgs // extends EventArgs
{
	public SegmentStage Stage = SegmentStage.forValue(0);
	public String Info = "";

	public SegmentEventArgs() {
	}

	public SegmentEventArgs(SegmentStage stage, String info) {
		this.Stage = stage;
		this.Info = info;
	}
}