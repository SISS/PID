package csiro.pidsvc.mappingstore.condition.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrioritizedQueue
{
	protected class Item
	{
		public final String Value;
		public final int Order;
		public float QFactor = 0;

		public Item(String value, int order, float qFactor)
		{
			Value = value;
			Order = order;
			QFactor = qFactor;
		}
		public String toString()
		{
			return Value + "*" + QFactor + "@" + Order;
		}
	}

	protected class ItemComparator implements Comparator<Item>
	{
		@Override
		public int compare(Item o1, Item o2)
		{
			if (o1.QFactor > o2.QFactor)
				return -1;
			else if (o1.QFactor < o2.QFactor)
				return 1;
			else if (o1.Order > o2.Order)
				return 1;
			else if (o1.Order < o2.Order)
				return -1;
			return 0;
		}
	}

	protected final ArrayList<String> _queue;

	public PrioritizedQueue(String in)
	{
		String				values[] = in.split(",\\s*");
		ArrayList<Item>		queue = new ArrayList<Item>(values.length);
		float				qFactor;
		Pattern				re = Pattern.compile("^([^;]+)(?:;q=(.+))$", Pattern.CASE_INSENSITIVE);
		Matcher				m;
		Item				item;

		for (int i = 0; i < values.length; ++i)
		{
			m = re.matcher(values[i]);
			if (m.find())
			{
				try
				{
					qFactor = Float.parseFloat(m.group(2));
				}
				catch (NumberFormatException ex)
				{
					qFactor = 0;
				}
				queue.add(new Item(m.group(1), i, qFactor));
			}
			else
				queue.add(new Item(values[i], i, 0));
		}

		// Fix the Q factors.
		qFactor = 0;
		for (int i = queue.size() - 1; i >= 0; --i)
		{
			item = queue.get(i); 
			if (item.QFactor == 0)
				item.QFactor = qFactor;
			else
				qFactor = item.QFactor;
		}

		// Sort the queue.
		Collections.sort((List<Item>)queue, new ItemComparator());

		_queue = new ArrayList<String>(values.length);
		for (Item it : queue)
			_queue.add(it.Value);
	}

	public List<String> getQueue()
	{
		return _queue;
	}
}
