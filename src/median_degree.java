/**
 * Author: Sam Nguyen
 * Date: 7/11/2016
 * 
 * This program was created by the author for Insight Data Engineering's
 * coding challenge for the fall fellowship. A description of this
 * program's function is described here:
 * https://github.com/InsightDataScience/coding-challenge
 */

import java.io.*;
import java.util.*;
public class median_degree {
	public static void main(String[] args) throws FileNotFoundException {
		String path = "../venmo_input/venmo-trans.txt";
		String output = "../venmo_output/output.txt";
		
		ArrayList<String> input = new ArrayList<String>();
		
		//get all input lines into array list to save cycles going back to file
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			String add = "not null!";
			while((add = reader.readLine()) != null)
				input.add(add);
		} catch (FileNotFoundException e) {
			System.err.println("File Not Found! Terminating.");
			System.exit(0);
		} catch (IOException e) {
			System.err.println("Error reading input! Terminating.");
			System.exit(0);
		}
		
		/* CONCEPT: We will store the times inside a sorted array. After
		 * a new payment comes in, we check to see if the oldest time
		 * is still within a minute of the new time, and update the
		 * graph if necessary. The times will be associated with a 
		 * pair of strings (target and actor), tied into a Transfer
		 * object, so we know how to update
		 * the graph
		 */
		
		HashMap<String, Node> nodes = new HashMap<String, Node>(); //the people
		ArrayList<Transfer> heap = new ArrayList<Transfer>(); //transfers sorted by time
		ArrayList<Int> degrees = new ArrayList<Int>(); //degrees of nodes in graph
		PrintWriter writer = new PrintWriter(output);
		
		for(int i = 0; i < input.size(); i++) {
			//get time, actor, and target information
			String cur = input.get(i);
			String[] sections = cur.split(",");
			int pointer = sections[1].indexOf(":");
			String name2 = sections[1].substring(pointer+3, sections[1].length()-1);
			pointer = sections[2].indexOf(":");
			//check if actor field is empty, if so continue;
			if(sections[2].substring(pointer, sections[2].length()).length() <= 3) continue;
			String name1 = sections[2].substring(pointer+3, sections[2].length()-2);
			pointer = sections[0].indexOf(":");
			String time = sections[0].substring(pointer+3, sections[0].length()-2);			
			long actualTime = getTime(time);
			
			//work with the graph, updating degrees and heap if necessary
			if(heap.size() == 0) { //just add it
				Transfer current = new Transfer(actualTime, name1, name2);
				heap.add(current);
				Transfer.makeConnection(nodes, degrees, name1, name2);
			} else {
				Transfer current = new Transfer(actualTime, name1, name2);
				long minTime = heap.get(0).time;
				if(actualTime - minTime >= 60) { //eviction time
					while(actualTime - minTime >= 60) {
						Transfer deleting = heap.get(0);
						heap.remove(0);
						removal(nodes, degrees, deleting.actor, deleting.target);
						if(heap.size() == 0) {
							minTime = actualTime;
							break;
						}
						minTime = heap.get(0).time;
					}
				}
				/*
				 * if the current transfer's time is earlier than the
				 * heap's earliest transfer, check with maximum timestamp
				 * to make sure we can process this transfer
				 */
				if(actualTime - minTime < 0) { 
					long max = heap.get(heap.size()-1).time;
					if(max - actualTime <= 60) {
						heap.add(current);
						Transfer.makeConnection(nodes, degrees, name1, name2);
					}
				} else { // 0 <= actualTime - minTime < 60
					heap.add(current);
					Transfer.makeConnection(nodes, degrees, name1, name2);
				}
				Collections.sort(heap, new TransferSort());
			}
			
			Collections.sort(degrees, new IntSort());
			float median;
			if(degrees.size() % 2 == 0) {
				if(degrees.size() == 2) {
					median = (degrees.get(0).val + degrees.get(1).val) / 2;
					//System.out.printf("%.2f\n", median);
					writer.printf("%.2f\n", median);
				} else {
					int temp = degrees.size()/2-1;
					median = (float)(degrees.get(temp).val+degrees.get(temp+1).val)/2;
					//System.out.printf("%.2f\n", median);
					writer.printf("%.2f\n", median);
				}
			} else {
				median = (float)degrees.get(degrees.size()/2).val;
				//System.out.printf("%.2f\n", median);
				writer.printf("%.2f\n", median);
			}
		}
		writer.close();
	}
	
	/**
	 * This method takes care of the removal process of an old Transfer from the graph
	 * @param nodes HashMap of nodes
	 * @param degrees List of degrees in graph
	 * @param name1 name of node A
	 * @param name2 name of node B
	 */
	public static void removal(HashMap<String, Node> nodes, ArrayList<Int> degrees, String name1, String name2) {
		Node one = nodes.get(name1);
		Node two = nodes.get(name2);
		
		one.edges.put(name2, one.edges.get(name2)-1);
		two.edges.put(name1, two.edges.get(name1)-1);
		
		if(one.edges.get(name2) == 0) {
			one.edges.remove(name2);
			one.degree.decr();
			two.edges.remove(name1);
			two.degree.decr();
			
			if(one.degree.val == 0) {
				degrees.remove(one.degree);
			}
			if(two.degree.val == 0) {
				degrees.remove(two.degree);
			}
		}
	}
	
	/**
	 * This method converts the time of transfer, including the date, into usable, sortable data
	 * @param time The time in 77-mm-ddThh:mm:ssZ format
	 * @return the usable time
	 */
	public static long getTime(String time) {
		String[] partition = time.split("-");
		int days = Integer.parseInt(partition[0]);
		days *= 365;
		days += ( (Integer.parseInt(partition[1]) -1) *30);
		partition = partition[2].split(":");
		days += Integer.parseInt(partition[0].substring(0, 2))-1;
		long seconds = Integer.parseInt(partition[0].substring(3, 5))*3600;
		seconds += (Integer.parseInt(partition[1])*60);
		seconds += Integer.parseInt(partition[2].substring(0, 2));
		days = days * 24 * 60 * 60; //24 hrs/day, 60 min/hr, 60sec/min
		seconds += days;
		return seconds;	
	}

	/**
	 * This Node class contains a person's information, more specifically
	 * their connections, their name, and their current degree of connections.
	 *
	 */
	public static class Node {
		private String name;
		private HashMap<String, Integer> edges;
		Int degree;
		
		public Node(String n) {
			name = n;
			edges = new HashMap<String, Integer>();
			degree = new Int(0);
		}
		
	}
	
	/**
	 * An integer wrapper class that can be passed by reference
	 *
	 */
	public static class Int {
		int val;
		
		public Int(int v) {
			val = v;
		}
		
		public void incr() {
			val++;
		}
		
		public void decr() {
			val--;
		}
	}
	
	/**
	 * This class holds information pertaining to a venmo transfer
	 */
	public static class Transfer {
		long time;
		String actor;
		String target;
		
		public Transfer(long time2, String actor2, String target2) {
			time = time2;
			actor = actor2;
			target = target2;
		}
		
		public static void makeConnection(HashMap<String, Node> nodes, ArrayList<Int> degrees, String name1, String name2) {
			Node process;
			if(nodes.containsKey(name1)) {
				process = nodes.get(name1);
				if(process.degree.val == 0) degrees.add(process.degree);
			} else {
				process = new Node(name1);
				nodes.put(name1,  process);
				degrees.add(process.degree);
			}
			if(process.edges.containsKey(name2))
				process.edges.put(name2, process.edges.get(name2) +1);
			else {
				process.edges.put(name2, 1);
				process.degree.incr();
			}
			if(nodes.containsKey(name2)) {
				process = nodes.get(name2);
				if(process.degree.val == 0) degrees.add(process.degree);
			} else {
				process = new Node(name2);
				nodes.put(name2,  process);
				degrees.add(process.degree);
			}
			if(process.edges.containsKey(name1))
				process.edges.put(name1, process.edges.get(name1) +1);
			else {
				process.edges.put(name1, 1);
				process.degree.incr();
			}
		}
	}
	
	/**
	 * Comparator to sort Int class objects
	 */
	static class IntSort implements Comparator<Int> {

		public int compare(Int arg0, Int arg1) {
			if(arg0.val <= arg1.val) return -1;
			else return 0;
		}
	}
	
	/**
	 * Comparator to sort Transfer class objects
	 */
	static class TransferSort implements Comparator<Transfer> {

		public int compare(Transfer arg0, Transfer arg1) {
			return Long.compare(arg0.time, arg1.time);
		}
		
	}
}
