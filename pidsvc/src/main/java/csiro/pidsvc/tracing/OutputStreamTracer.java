package csiro.pidsvc.tracing;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamTracer implements ITracer
{
	protected final OutputStream _stream;
	protected final long _start;

	public OutputStreamTracer(OutputStream stream)
	{
		_start = System.currentTimeMillis();
		_stream = stream;
	}

	@Override
	public void trace(String message)
	{
		try
		{
			printTiming();
			_stream.write('\t');
			_stream.write(message.getBytes());
			_stream.write('\n');
		}
		catch (IOException e)
		{
		}
	}

	protected void printTiming() throws IOException
	{
		long diff = System.currentTimeMillis() - _start;
		long ms = (diff >= 1000 ? diff % 1000 : diff);
		long secs = (diff = (diff / 1000)) >= 60 ? diff % 60 : diff;
		long mins = (diff = (diff / 60)) >= 60 ? diff % 60 : diff; 
		long hrs = (diff = (diff / 60));
		_stream.write(String.format("%02d:%02d:%02d.%03d", hrs, mins, secs, ms).getBytes());
	}
}
