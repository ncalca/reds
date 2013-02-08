package polimi.reds.context.gui;

import java.awt.TextArea;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class TextAreaHandler extends StreamHandler {
	private TextArea textArea;

	public TextAreaHandler( TextArea textArea ) {
		this.textArea = textArea;
	}

	@Override
	public synchronized void publish( LogRecord s ) {
		super.publish( s );
		textArea.setText( textArea.getText() + s.getMessage() + "\n" );
		textArea.setCaretPosition( textArea.getText().length() );
	}
}
