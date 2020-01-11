import java.awt.*;
import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 *
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD);
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;

	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;

	public HuffProcessor() {
		this(0);
	}

	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */

	private int[] readForCounts(BitInputStream in){
		int[] ret = new int[257];
		ret[PSEUDO_EOF] = 1;
		while(true){
			int bit = in.readBits(BITS_PER_WORD);
			if(bit == -1) return ret;
			ret[bit]++;
			}
		//this code tallies up the total number of occurances of each bit in the code.
		}

	private HuffNode makeTreeFromCounts(int[] counts){
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for(int index = 0; index < counts.length; index++){
			if(counts[index] > 0){ //is this the right boolean guard? This was giving me an error before and I'm still not sure it's right.
				pq.add(new HuffNode(index, counts[index], null, null));
			}
		}
		pq.add(new HuffNode(PSEUDO_EOF, 1));
		while(pq.size() > 1){
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			HuffNode t = new HuffNode(0, left.myWeight + right.myWeight, left, right);
			//I am assumiing that if it's not  leaf the value should be zero. I think that's how the rule goes.
			pq.add(t);
		}
		HuffNode root = pq.remove();
		return root;
		//this function generates an optimal hufftree from the total number of occurances of each bit in the code.

		//what this means is that it puts all the huffnodes (individual nodes with # of occurances counts[index]
		//and the ASCII value of the encoded letter (there's byzantine code somewhere that allows you to turn this into binary later)
		//it also creates myweight by making an individual node weight {num occurancex} and then adding each subsequent node
		//you use it here to generate the optimal huffman node
	}


	private void codingHelper(HuffNode root, String path, String[] encodings){
		if(root == null) return;
		if(root.myLeft == null && root.myRight == null){
			encodings[root.myValue] = path;
			return;
		}
		codingHelper(root.myLeft, path+"0", encodings);
		codingHelper(root.myRight, path+"1", encodings);
		/*
		given some tree with encodings of letters in the leaves, it gives you the path to that encoding at the ASCII index
		and puts it in encodings
		This is making that mythical 2nd tree I couldn't understand, the one where left is 0 and right is 1
		 */
	}

	private String[] makeCodingsFromTree(HuffNode root){
		String[] codings = new String[ALPH_SIZE + 1];
		codingHelper(root, "", codings);
		return codings;
		//this function then returns the list with encodings generated in the previous function.
	}

	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out){
		while(true){
			int bit = in.readBits(BITS_PER_WORD);
			if(bit == -1) break;
			String code = codings[bit];
			out.writeBits(code.length(), Integer.parseInt(code, 2));
		}
		out.writeBits(codings[PSEUDO_EOF].length(), Integer.parseInt(codings[PSEUDO_EOF], 2));
		//we are taking the bits and the encoding paths we made and writing that to out instead of the original 8 bit binary
		//this takes each letter in in and puts the path you'd have to take through the tree we created instead, in out.
	}
	private void writeTree(HuffNode root, BitOutputStream out){
		if(root.myLeft != null || root.myRight != null){
			out.writeBits(1, 0);
			writeTree(root.myLeft, out);
			writeTree(root.myRight, out);
		}
		else{
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, root.myValue);
		}
	}
	//this writes the tree out in binary as an in-order traversal where all interior nodes are zero
	//and all leafnodes are a 1 followed by the original, non-compressed, true to ASCII binary value they encode.


	public void compress(BitInputStream in, BitOutputStream out){
		int[] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);
		out.writeBits(BITS_PER_INT, HUFF_TREE);

		writeTree(root, out);
		//this writes the tree tree created by writeTree (original, non-compressed, true to ASCII binary value tree)
		//into out.

		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();
		//in.reset puts in back at the beginning
	}



	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 */



	private HuffNode readTree(BitInputStream in) { //throws IOException {
		int bit = in.readBits(1);
		if(bit == -1) {
			throw new HuffException("Invalid bit value");
		}
		if(bit == 0){
			HuffNode left = readTree(in); //I want in starting from next bit
			HuffNode right = readTree(in); //Here I want in starting from next.next bit.
			return new HuffNode(0,0, left, right);
			//returns the root of the huffTree. This is constructing a tree, not reading one.
		}
		else{
			int value = in.readBits(BITS_PER_WORD + 1);
			return new HuffNode(value, 0, null, null);
			//I don't understand how these return values aren't being discarded.
		}
		//this builds the hufftree from the binary encoded version in the BitInputFile
	}

	public void decompress(BitInputStream in, BitOutputStream out){
		//input stream has three parts. Magic number shit, then pre-order traversal of the tree.
		//0's denote every single internal node. Each leaf node gets a 1 and then nine bits which describe the node value.
		int magic = in.readBits(BITS_PER_INT);
		if (magic != HUFF_TREE) {
			throw new HuffException("invalid magic number "+magic);
		}
		HuffNode root = readTree(in);
		//the data itself, which represents the text, is entirely separate from this, and the
		//and root is a map for decoding the data. Huffman code is making a custom tree and encoding the data according to that tree.
		HuffNode current = root;
		//out.writeBits(BITS_PER_INT,magic);
		while (true){
			int val = in.readBits(1);
			if (val == -1){
				throw new HuffException("No PSEUDO_EOF");
			}
			else{
				if(val == 0) current = current.myLeft;
				//val is the physical binary digits in the inputBitfile.
				//if val is zero go left, if one go right. The decoded tree reaches a leaf then write the value stored in the
					// tree and then go back to the top of the tree and start over
				else{
					current = current.myRight;
				}
				//I'm getting a null pointer exception here. that means current is null. That's a problem.
				//But Idk if that's a problem with my decompress or compress. I assume it's compress. But how to fix that?
				if(current.myLeft  == null && current.myRight == null){
					if(current.myValue == PSEUDO_EOF) break;
					else{
						//write 8 bits for current value. What the hell does that mean?
						out.writeBits(BITS_PER_WORD, current.myValue);
						current = root;
					}
				}
			}
		}
		out.close();
	}
}
