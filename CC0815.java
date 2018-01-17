/* CC0815
 * Alex Gruschina, Mario Kotoy
 */

import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.IOException;

public class CC0815 {

	// define records
	class Token {
		int symbolId;
		int number;
		int lineNumber;
		String identifier;
	}

	class Type {
		int form; // 0 = integer; 1 = character; 2 = string; 3 = boolean; 4 =
					// array; 5 = record; 6 = void, 7 = unknown
		int length;
		SymbolNode field;
		Type base;
	}

	class SymbolNode {
		String name;
		int offset;
		int size;
		int nodeClass; // 0 = variable; 1 = type; 2 = method;
		Type type;
		String scope; // contains procedureContext, or "global", if global
		SymbolNode next;
		SymbolNode params;
	}

	class Item {
		public Item() {
			scopeReg = 28;
		}

		int mode; // 0-var, 1-const, 2-reg, 3-ref, 4-cond
		Type iType;
		int register;
		int offset;
		int value;
		int fls; // need for boolean expressions
		int tru; // need for boolean expressions
		int operator;
		int scopeReg;
	}

	boolean array;
	// ///////////////
	int INT = 0;
	int CHAR = 1;
	int STRING = 2;
	int BOOLEAN = 3;
	int VOID = 4;
	int STATIC = 5;
	int CLASS = 6;
	int WHILE = 7;
	int IF = 8;
	int ELSE = 9;
	int TRUE = 10;
	int FALSE = 11;
	int NEW = 12;
	int PUBLIC = 13;
	int RETURN = 14;
	int SYSTEM = 15;
	int NULL = 16;

	// ////////////////
	int LEFTPAR = 50;
	int RIGHTPAR = 51;
	int LEFTCURL = 52;
	int RIGHTCURL = 53;
	int LEFTBRACK = 54;
	int RIGHTBRACK = 55;
	int SEMICOL = 56;
	int DOT = 57;
	int COMMA = 58;
	int COLON = 59;
	int SINGLEQUOTE = 60;
	int DOUBLEQUOTE = 61;
	int PLUS = 62;
	int MINUS = 63;
	int TIMES = 64;
	int SLASH = 65;
	int GREATER = 66;
	int LESS = 67;
	int BANG = 68; // "Ausrufezeichen"
	int BINAND = 69;
	int BINOR = 70;
	int EQUALS = 71;
	int GTE = 72; // >=
	int LTE = 73; // <=
	int NOTEQUALS = 74; // !=
	int AND = 75;
	int OR = 76;
	int DOUBLEEQUALS = 77; // ==

	// ////////////////
	int NUMBER = 101;
	int IDENTIFIER = 102;
	int EOF = 99;

	// global used variables
	String sourceFileName;
	int heapPointer;
	int[] regs;
	Item leftItem;
	Item rightItem;
	SymbolNode tempMain;
	Token nextToken;
	Type integer;
	Type character;
	Type string;
	Type bool;
	Type record;
	Type vd;
	Type unknownType;

	// variables used by scanner
	int i; // variable is used for loop-break-condition
	String operator; // temporary variable to check if symbol is operator
	int currentChar;
	int nextChar;
	FileReader fr;
	String[] keywords;
	String[] operators;

	// variables used by the parser
	byte[] byteInstr;
	int intInstr;
	FileOutputStream fw;
	byte[] buff; // we need this byte buffer for dealing with PC
	int buffSize;
	int PC;
	Token token;
	String tempIdent;
	int tempNumber;
	int offsetCounter;
	int localOffsetCounter;
	int numOfErrors;
	int returnFJumpAddress;
	SymbolNode procedureContext;
	boolean isSyso;

	// variables used by the symbol-table
	Type tempType;
	SymbolNode root;
	SymbolNode localRoot;
	SymbolNode localRootBackup;
	SymbolNode tempNode;
	SymbolNode localTempNode;
	SymbolNode fieldNode;

	/************************************************************************************************
	 **** the scanner of the compiler *** reads the inputFile and separates the
	 * text into tokens ****
	 ************************************************************************************************/

	// creates Scanner and initialize variables
	public void setScanner(String fileName) throws Exception {
		keywords = new String[17];
		keywords[0] = "int";
		keywords[1] = "char";
		keywords[2] = "String";
		keywords[3] = "boolean";
		keywords[4] = "void";
		keywords[5] = "static";
		keywords[6] = "class";
		keywords[7] = "while";
		keywords[8] = "if";
		keywords[9] = "else";
		keywords[10] = "true";
		keywords[11] = "false";
		keywords[12] = "new";
		keywords[13] = "public";
		keywords[14] = "return";
		keywords[15] = "System";
		keywords[16] = "null";

		operators = new String[22];
		operators[0] = "(";
		operators[1] = ")";
		operators[2] = "{";
		operators[3] = "}";
		operators[4] = "[";
		operators[5] = "]";
		operators[6] = ";";
		operators[7] = ".";
		operators[8] = ",";
		operators[9] = ":";
		operators[10] = "'";
		operators[11] = "\"";
		operators[12] = "+";
		operators[13] = "-";
		operators[14] = "*";
		operators[15] = "/";
		operators[16] = ">";
		operators[17] = "<";
		operators[18] = "!";
		operators[19] = "&";
		operators[20] = "|";
		operators[21] = "=";

		sourceFileName = fileName.substring(0, fileName.length() - 5);
		fr = new FileReader(fileName);
		nextToken = new Token();
		nextToken.lineNumber = 1;
		nextToken.symbolId = 65535;
		currentChar = 65535;
		nextChar = 65535;
	}

	/*
	 * checks if the file is empty if empty return end of file else create
	 * nextToken and return
	 */
	public void getToken() throws Exception {
		nextToken.identifier = "";
		nextToken.number = 65535;
		if (currentChar == 65535) {
			currentChar = fr.read();
			nextChar = fr.read();
		} else if (currentChar == -1) {
			fr.close();
			nextToken.symbolId = EOF;
		}
		// skip whitespace
		while (currentChar == 9 || currentChar == 10 || currentChar == 32) {
			if (currentChar == 10)
				nextToken.lineNumber = nextToken.lineNumber + 1;
			getNextChar();
		}
		// end of file?
		if (currentChar == -1) {
			fr.close();
			nextToken.symbolId = EOF;
			// is comment?
		} else if (currentChar == 47 && (nextChar == 42 || nextChar == 47)) {
			skipComment();
			if (currentChar == -1) {
				fr.close();
				nextToken.symbolId = EOF;
			}
		}
		if (currentChar == -1) {
			fr.close();
			nextToken.symbolId = EOF;
			// is identifier?
		} else if (currentChar >= 65 && currentChar <= 90 || currentChar >= 97
				&& currentChar <= 122) {
			while (currentChar >= 65 && currentChar <= 90 || currentChar >= 97
					&& currentChar <= 122 || currentChar >= 48
					&& currentChar <= 57) {
				nextToken.identifier = nextToken.identifier
						+ (char) currentChar;
				getNextChar();
			}
			if (currentChar == -1) {
				fr.close();
				nextToken.symbolId = EOF;
			} else {
				i = 0;
				// is identifier or keyword?
				while (i < keywords.length) {
					if (keywords[i].equals(nextToken.identifier)) {
						nextToken.symbolId = i;
						i = keywords.length;
					} else {
						nextToken.symbolId = IDENTIFIER;
						i = i + 1;
					}
				}
			}
			// is number?
		} else if (currentChar >= 48 && currentChar <= 57) {
			i = 1;
			nextToken.number = 0;
			while (currentChar >= 48 && currentChar <= 57) {
				nextToken.number = nextToken.number * 10 + (currentChar - 48);
				getNextChar();
			}
			if (currentChar == -1) {
				fr.close();
				nextToken.symbolId = EOF;
			} else {
				nextToken.symbolId = NUMBER;
			}
			// is operator?
		} else {
			operator = "";
			operator = operator + (char) currentChar;
			i = 0;
			while (i < operators.length) {
				if (operators[i].equals(operator)) {
					nextToken.identifier = nextToken.identifier + operators[i];
					nextToken.symbolId = i + 50;
				}
				i = i + 1;
			}
			getNextChar();
			operator = "";
			operator = operator + (char) currentChar;
			i = 19;
			while (i < 22) {
				if (operators[i].equals(operator)) {
					nextToken.identifier = nextToken.identifier + operators[i];
					nextToken.symbolId = nextToken.symbolId + 6;
					getNextChar();
				}
				i = i + 1;
			}
		}
	}

	public void getNextChar() throws Exception {
		currentChar = nextChar;
		nextChar = fr.read();
	}

	public void skipComment() throws Exception {
		if (currentChar == 47 && nextChar == 47) {
			while (!(currentChar == 10 || currentChar == -1)) {
				getNextChar();
			}
			if (currentChar == 10) {
				nextToken.lineNumber = nextToken.lineNumber + 1;
			}
			getNextChar();
		} else if (currentChar == 47 && nextChar == 42) {
			getNextChar();
			getNextChar();
			while (!(currentChar == 42 && nextChar == 47) || currentChar == -1) {
				if (currentChar == 10) {
					nextToken.lineNumber = nextToken.lineNumber + 1;
				}
				getNextChar();
			}
			getNextChar();
			getNextChar();
		}
		// skip whitespace
		while (currentChar == 9 || currentChar == 10 || currentChar == 32) {
			if (currentChar == 10) {
				nextToken.lineNumber = nextToken.lineNumber + 1;
			}
			getNextChar();
		}
		if (currentChar == 47 && (nextChar == 42 || nextChar == 47)) {
			skipComment();
		}
	}

	/*************************************************************************************************
	 *********************************** the parser of the compiler ********************************** checks if
	 * the syntax and semantic is correct and creates byte-code for target
	 * machine (DLX) **
	 *************************************************************************************************/

	public void setParser() throws Exception {
		byteInstr = new byte[4];
		heapPointer = 4;
		offsetCounter = 0;
		localOffsetCounter = 0;
		regs = new int[32];
		numOfErrors = 0;
		buff = new byte[30000];
		PC = 0;
		isSyso = false;
		fw = new FileOutputStream("dlxcode.bin");
		token = new Token();
		getToken();
		nextToken();
		setSymbolTable();
	}

	public int requestReg() {
		i = 1;
		while (i < 27) {
			if (regs[i] == 0) {
				regs[i] = 1;
				return i;
			}
			i = i + 1;
		}
		return -1;
	}

	void releaseReg(int reg) {
		regs[reg] = 0;
	}

	public void writeCode() throws IOException {
		mainProcHandling();
		int i = 0;
		while (i < buff.length) {
			if (i < PC - 1) {
				// instruction @ PC-1 : JMP to Main! (written to file first)
				fw.write(buff[i * 4]);
				fw.write(buff[i * 4 + 1]);
				fw.write(buff[i * 4 + 2]);
				fw.write(buff[i * 4 + 3]);
			}
			i = i + 1;
		}
	}

	public void mainProcHandling() throws IOException {
		// First create JMP to main procedure
		SymbolNode mainProc;
		mainProc = getNode("main");
		putF3(17, mainProc.offset + 1);
		fw.write(buff[(PC - 1) * 4]);
		fw.write(buff[(PC - 1) * 4 + 1]);
		fw.write(buff[(PC - 1) * 4 + 2]);
		fw.write(buff[(PC - 1) * 4 + 3]);
	}

	public void encodeC(int branchAddress, int newBranchAddress) {
		int c;
		c = buff[branchAddress * 4 + 3];
		int oldC;
		oldC = c;
		c = c >>> 5;
		c = c << 5;
		c = c | newBranchAddress;
		// System.out.println("@ line " + token.lineNumber
		// + "\t[ENCODE C] previous = " + oldC + " to " + c);
		buff[branchAddress * 4 + 3] = (byte) c;
	}

	public int decodeC(int branchAddress) {
		// System.out.println("Decode with branchAddress: " + branchAddress
		// + " @ line: " + token.lineNumber);
		int c;
		c = buff[branchAddress * 4 + 3];
		// System.out.println("C: " + c);
		int address;
		address = c;
		c = c << 3;
		c = c >>> 3;
		// System.out.println("@ line " + token.lineNumber +
		// "\t[DECODE C] from "
		// + address + " to " + c);

		return c;
	}

	public void putF1(int opCode, int rx, int ry, int c)
			throws IOException {
		opCode = opCode << 26;
		rx = rx << 21;
		ry = ry << 16;
		if (c < 0) 
			c = c ^ 0xFFFF0000;
		intInstr = opCode | rx | ry | c;
		buff[PC * 4] = (byte) (intInstr >>> 24);
		buff[PC * 4 + 1] = (byte) (intInstr >>> 16);
		buff[PC * 4 + 2] = (byte) (intInstr >>> 8);
		buff[PC * 4 + 3] = (byte) intInstr;
		PC = PC + 1;
	}

	public void putF2(int opCode, int rx, int ry, int rz)
			throws IOException {
		// System.out.println("PC " + PC + " opcode = " + opCode + " @line "
		// + token.lineNumber);
		opCode = opCode << 26;
		rx = rx << 21;
		ry = ry << 16;
		rz = rz << 11;
		intInstr = opCode | rx | ry | rz;
		buff[PC * 4] = (byte) (intInstr >>> 24);
		buff[PC * 4 + 1] = (byte) (intInstr >>> 16);
		buff[PC * 4 + 2] = (byte) (intInstr >>> 8);
		buff[PC * 4 + 3] = (byte) intInstr;

		PC = PC + 1;
	}

	public void putF3(int opCode, int c) throws IOException {
		// System.out.println("PC " + PC + " opcode = " + opCode + " @line "
		// + token.lineNumber);
		if (c < 0) {
			// c = (((65535) & c) | (-65356 & (c * (-1) - 1)));
			c = c ^ 0xFFFF0000;
		}
		opCode = opCode << 26;
		intInstr = opCode | c;
		buff[PC * 4] = (byte) (intInstr >>> 24);
		buff[PC * 4 + 1] = (byte) (intInstr >>> 16);
		buff[PC * 4 + 2] = (byte) (intInstr >>> 8);
		buff[PC * 4 + 3] = (byte) intInstr;

		PC = PC + 1;
	}

	public void parseClass() throws Exception {
		while (token.symbolId != PUBLIC && token.symbolId != EOF) {
			nextToken();
		}
		nextToken();
		nextToken();
		if (!token.identifier.equals(sourceFileName))
			error("class name should be same as file name.");
		nextToken();
		nextToken();
		while (token.symbolId != RIGHTCURL && token.symbolId != EOF) {
			parseClassBody();
		}
	}

	public void parseClassBody() throws Exception {
		while (token.symbolId != PUBLIC && token.symbolId != EOF) {
			parseDeclaration();
		}
		// while (nextToken.symbolId != STATIC && token.symbolId != EOF) {
		// procedureImplementation();
		// // nextToken();
		// // nextToken();
		// // parseMethDec();
		// }
		// parseMain();
		while (token.symbolId != RIGHTCURL) {
			procedureImplementation();
		}
	}

	public void parseDeclaration() throws Exception {
		if (token.symbolId == IDENTIFIER && nextToken.symbolId == IDENTIFIER) {
			addRecord(nextToken.identifier, token.identifier);
			nextToken();
			nextToken();
			if (token.symbolId == SEMICOL) {
				nextToken();
			}
		} else if (token.symbolId < 4 && nextToken.symbolId == IDENTIFIER) {
			if (token.symbolId == INT) {
				addInteger(nextToken.identifier);
			}
			if (token.symbolId == CHAR) {
				addCharacter(nextToken.identifier);
			}
			if (token.symbolId == STRING) {
				addString(nextToken.identifier);
			}
			if (token.symbolId == BOOLEAN) {
				addBoolean(nextToken.identifier);
			}
			nextToken();
			nextToken();
			if (token.symbolId == SEMICOL) {
				nextToken();
			}
		} else if (token.symbolId < 4 && nextToken.symbolId == LEFTBRACK) {
			if (token.symbolId == INT) {
				nextToken();
				nextToken();
				nextToken();
				addArray(token.identifier, integer, -4);
			}
			if (token.symbolId == CHAR) {
				nextToken();
				nextToken();
				nextToken();
				addArray(token.identifier, character, -4);
			}
			if (token.symbolId == STRING) {
				nextToken();
				nextToken();
				nextToken();
				addArray(token.identifier, string, -4);
			}
			if (token.symbolId == BOOLEAN) {
				nextToken();
				nextToken();
				nextToken();
				addArray(token.identifier, bool, -4);
			}
			nextToken();
			nextToken();
			if (token.symbolId == SEMICOL) {
				nextToken();
			}
		} else if ((token.symbolId < 4 || contains(token.identifier))
				&& nextToken.symbolId == LEFTBRACK) {

			if (getNode(token.identifier).type != null
					&& getNode(token.identifier).type.form == 5) {
				SymbolNode node;
				node = getNode(token.identifier);
				nextToken();

				while (nextToken.symbolId == RIGHTBRACK) {
					nextToken();
					nextToken();
				}

				if (token.symbolId == IDENTIFIER) {
					addArray(token.identifier, node.type, -4, node.size);
					nextToken();
				}
				if (token.symbolId == SEMICOL) {
					nextToken();
				}
			}
			if (token.symbolId == INT) {
				nextToken();
				while (nextToken.symbolId == RIGHTBRACK) {
					nextToken();
					nextToken();
				}

				if (token.symbolId == IDENTIFIER) {
					addArray(token.identifier, integer, -4);
					nextToken();
				}
				if (token.symbolId == SEMICOL) {
					nextToken();
				}
			}
			if (token.symbolId == CHAR) {
				nextToken();
				while (nextToken.symbolId == RIGHTBRACK) {
					nextToken();
					nextToken();
				}
				if (token.symbolId == IDENTIFIER) {
					addArray(token.identifier, character, -4);
					nextToken();
				}
				if (token.symbolId == SEMICOL) {
					nextToken();
				}
			}
			if (token.symbolId == STRING) {
				nextToken();
				while (nextToken.symbolId == RIGHTBRACK) {
					nextToken();
					nextToken();
				}
				if (token.symbolId == IDENTIFIER) {
					addArray(token.identifier, string, -4);
					nextToken();
				}
				if (token.symbolId == SEMICOL) {
					nextToken();
				}
			}
			if (token.symbolId == BOOLEAN) {
				nextToken();
				while (nextToken.symbolId == RIGHTBRACK) {
					nextToken();
					nextToken();
				}
				if (token.symbolId == IDENTIFIER) {
					addArray(token.identifier, bool, -4);
					nextToken();
				}
				if (token.symbolId == SEMICOL) {
					nextToken();
				}
			}
		} else if (token.symbolId == CLASS) {
			nextToken();
			if (token.symbolId == IDENTIFIER) {
				tempIdent = token.identifier;
			}
			if (!contains(token.identifier)) {
				addRecType(token.identifier);
			}
			nextToken();
			if (token.symbolId == LEFTCURL) {
				nextToken();
			}
			while (token.symbolId != RIGHTCURL) {
				if (token.symbolId == IDENTIFIER) {
					if (contains(token.identifier)) {
						addField(tempIdent, nextToken.identifier,
								getNode(token.identifier).type, "", 4);
						nextToken();
						nextToken();
						if (token.symbolId == SEMICOL) {
							nextToken();
						}
					} else {
						addRecType(token.identifier);
						addField(tempIdent, nextToken.identifier, null,
								token.identifier, 0);
						nextToken();
						nextToken();
						if (token.symbolId == SEMICOL) {
							nextToken();
						}
					}
				} else if (token.symbolId < 4
						&& nextToken.symbolId == IDENTIFIER) {
					if (token.symbolId == INT) {
						nextToken();
						addField(tempIdent, token.identifier, integer, "", 4);
					}
					if (token.symbolId == CHAR) {
						nextToken();
						addField(tempIdent, token.identifier, character, "", 4);
					}
					if (token.symbolId == STRING) {
						nextToken();
						addField(tempIdent, token.identifier, string, "", 4);
					}
					if (token.symbolId == BOOLEAN) {
						nextToken();
						addField(tempIdent, token.identifier, bool, "", 4);
					}
					nextToken();
					if (token.symbolId == SEMICOL) {
						nextToken();
					}
				} else if (token.symbolId < 4
						&& nextToken.symbolId == LEFTBRACK) {
					tempType = new Type();
					tempType.form = 4;
					if (token.symbolId == INT) {
						tempType.base = integer;
						nextToken();
						// nextToken();
						while (nextToken.symbolId == RIGHTBRACK) {
							nextToken();

						}
						if (nextToken.symbolId == IDENTIFIER) {
							nextToken();

							addField(tempIdent, token.identifier, tempType, "",
									4);
							nextToken();
							if (token.symbolId == SEMICOL) {
								nextToken();
							}
						}
					}
					if (token.symbolId == CHAR) {
						tempType.base = character;
						nextToken();
						while (nextToken.symbolId == RIGHTBRACK) {
							nextToken();
						}
						if (nextToken.symbolId == IDENTIFIER) {
							nextToken();
							addField(tempIdent, token.identifier, tempType, "",
									4);
							nextToken();
							if (token.symbolId == SEMICOL) {
								nextToken();
							}
						}
					}
					if (token.symbolId == STRING) {
						tempType.base = string;
						nextToken();
						while (nextToken.symbolId == RIGHTBRACK) {
							nextToken();
						}
						if (nextToken.symbolId == IDENTIFIER) {
							nextToken();
							addField(tempIdent, token.identifier, tempType, "",
									4);
							nextToken();
							if (token.symbolId == SEMICOL) {
								nextToken();
							}
						}
					}
					if (token.symbolId == BOOLEAN) {
						tempType.base = bool;
						nextToken();
						while (nextToken.symbolId == RIGHTBRACK) {
							nextToken();
						}
						if (nextToken.symbolId == IDENTIFIER) {
							nextToken();
							addField(tempIdent, token.identifier, tempType, "",
									4);
							nextToken();
							if (token.symbolId == SEMICOL) {
								nextToken();
							}
						}
					}
				}
			}
			nextToken();
		}
	}

	public void parseStatementSequence() throws Exception {
		while (token.symbolId != RIGHTCURL && token.symbolId != EOF) {
			parseStatement();
		}
		if (token.symbolId == EOF) {
			error("Hit EOF early @ line " + token.lineNumber);
		}
	}

	public void parseStatement() throws Exception {
		if (isSyso) {
			nextToken();
			if (token.symbolId == DOUBLEQUOTE) {
				nextToken();
				while (token.symbolId != DOUBLEQUOTE) {
					if (token.symbolId == NUMBER) {
						putF1(34, 0, 0, token.number);
					} else {
						int i;
						i = 0;
						while (i < token.identifier.length()) {
							putF1(33, 0, 0, token.identifier.charAt(i));
							i = i + 1;
						}
					}
					nextToken();
					putF1(33, 0, 0, 0);
				}
				nextToken();
			} else {
				Item item;
				item = new Item();
				parseExpression(item);
				if (item.mode == 3) {
					putF2(31, item.register, item.scopeReg,
							item.register);
					item.mode = 2;
					putF1(36, 0, 0, item.register);
				} else
					putF1(35, 0, item.scopeReg, item.offset);
			}

			if (token.symbolId != PLUS) {
				nextToken();
				nextToken();
				putF1(33, 0, 0, '\n');
				isSyso = false;
			} else {
				isSyso = true;
			}
		} else if (token.symbolId == SYSTEM) {
			nextToken();
			nextToken();
			nextToken();
			nextToken();
			if (token.identifier.equals("println")) {
				nextToken();
				nextToken();
				if (token.symbolId == DOUBLEQUOTE) {
					nextToken();
					while (token.symbolId != DOUBLEQUOTE) {
						if (token.symbolId == NUMBER) {
							putF1(34, 0, 0, token.number);
						} else {
							int i;
							i = 0;
							while (i < token.identifier.length()) {
								putF1(33, 0, 0,
										token.identifier.charAt(i));
								i = i + 1;
							}
						}
						nextToken();
						putF1(33, 0, 0, 0);
					}
					nextToken();
				} else {
					Item item;
					item = new Item();
					parseExpression(item);
					if (item.mode == 3) {
						putF2(31, item.register, item.scopeReg,
								item.register);
						item.mode = 2;
						putF1(36, 0, 0, item.register);
					} else
						putF1(35, 0, item.scopeReg, item.offset);

				}
			}
			if (token.symbolId != PLUS) {
				nextToken();
				nextToken();
				putF1(33, 0, 0, '\n');
				isSyso = false;
			} else {
				isSyso = true;
			}
		} else if (token.symbolId == IDENTIFIER && nextToken.symbolId == EQUALS) {
			if (getNode(token.identifier).type.form == 4) {
				parseAllocation();

			} else if (getNode(token.identifier).type.form == 5) {
				parseAllocation();

			} else if (getNode(token.identifier).type.form < 4) {

				parseAssignment();
				nextToken();
			}
		} else if (token.symbolId == IDENTIFIER
				&& nextToken.symbolId == LEFTPAR) {
			Item item;
			item = new Item();
			procedureCall(item);
			if (token.symbolId != SEMICOL)
				errorAtToken(";", token);
			nextToken();
		} else if (token.symbolId == IDENTIFIER && nextToken.symbolId == PLUS) {
			Item item;
			item = new Item();
			parseFactor(item);
			if (nextToken.symbolId == PLUS) {
				if (item.mode == 0) {
					load(item);
					putF1(1, item.register, item.register, 1);
					putF1(8, item.register, item.scopeReg, item.offset);
					releaseReg(item.register);
				} else
					error("Expected identifier for using increment operator ++");
			} else {
				errorAtToken("+, increment-operator ++", nextToken);
			}
			releaseReg(item.register);
			nextToken();
			nextToken();
			nextToken();
		} else if (token.symbolId == WHILE) {
			parseWhileStatement();
		} else if (token.symbolId == IF) {
			parseIfStatement();
		} else if (token.symbolId == RETURN) {
			procedureReturn();
			if (token.symbolId == SEMICOL)
				nextToken();
		} else if (nextToken.symbolId == LEFTBRACK || nextToken.symbolId == DOT) {
			parseAssignment();
			nextToken();
		} else if (token.symbolId == IDENTIFIER && nextToken.symbolId == DOT) {
			error("Should not access this part!!!!!");
		} else {
			error("Unknown or misplaced token @ line " + token.lineNumber
					+ ". Skip until hitting ';' or '}'");
			while (token.symbolId != SEMICOL && token.symbolId != RIGHTCURL) {
				nextToken();
			}
			if (token.symbolId == SEMICOL) {
				nextToken();
			}
		}
	}

	private void parseAllocation() throws Exception {
		int register;
		SymbolNode node;

		node = getNode(token.identifier);
		nextToken();
		nextToken();
		if (token.symbolId == NEW) {
			nextToken();
		} else {
			errorAtToken("new", token);
		}
		if ((token.symbolId < 4 || contains(token.identifier))
				&& nextToken.symbolId == LEFTBRACK) {
			nextToken();
			nextToken();

			if (token.symbolId == NUMBER) {
				node.type.length = token.number;
				register = requestReg();
				putF1(1, register, 0, heapPointer);
				putF1(8, register, 28, node.offset);
				releaseReg(register);
				heapPointer = heapPointer + token.number * 4 * (node.size / 4);
				nextToken();
			} else {
				errorAtToken("number", token);
			}
			if (token.symbolId == RIGHTBRACK) {
				nextToken();
			} else {
				errorAtToken("]", token);
				while (token.symbolId != SEMICOL)
					nextToken();
				nextToken();

			}
			// System.out.println(token.symbolId);
			while (token.symbolId == LEFTBRACK) {
				nextToken();
				heapPointer = heapPointer + token.number * 4 * (node.size / 4);
				nextToken();
				nextToken();
			}
			if (token.symbolId == SEMICOL) {
				nextToken();
			} else {
				errorAtToken(";", token);
				while (token.symbolId != SEMICOL)
					nextToken();
				nextToken();
			}
		} else if (getNode(token.identifier).type.form == 5) {
			register = requestReg();
			putF1(1, register, 0, heapPointer);
			putF1(8, register, 28, node.offset);
			releaseReg(register);
			node = getNode(token.identifier);
			heapPointer = heapPointer + node.size;
			nextToken();
			if (token.symbolId != LEFTPAR) {
				errorAtToken("(", token);
			}
			nextToken();
			if (token.symbolId != RIGHTPAR) {
				errorAtToken(")", token);
			}
			nextToken();
			if (token.symbolId != SEMICOL) {
				errorAtToken(";", token);
			}
			nextToken();

		} else {
			error("Variable " + getNode(token.identifier)
					+ " should be a record");
		}
	}

	public void parseAssignment() throws Exception {
		Item leftItem;
		Item rightItem;
		leftItem = new Item();
		if (token.symbolId == IDENTIFIER) {
			parseFactor(leftItem);
		} else {
			errorAtToken("identifier", token);
		}

		nextToken();
		rightItem = new Item();

		if (token.symbolId == RIGHTBRACK || token.symbolId == RIGHTPAR) {
			nextToken();
			releaseReg(leftItem.register);
			releaseReg(rightItem.register);
		} else {
			parseExpression(rightItem);
			assignOperator(leftItem, rightItem);
		}
	}

	public void assignOperator(Item lItem, Item rItem) throws Exception {
		if (rItem.iType != null) {
			if (rItem.iType.form == 3) {
				unloadBool(rItem);
			}
		}
		if (lItem.mode == 0) {
			if (rItem.mode == 3) {
				putF2(31, rItem.register, rItem.scopeReg,
						rItem.register);
				putF1(8, rItem.register, lItem.scopeReg, lItem.offset);
			} else if (rItem.mode == 2) {
				putF1(8, rItem.register, lItem.scopeReg, lItem.offset);
			} else {
				load(rItem);
				putF1(8, rItem.register, lItem.scopeReg, lItem.offset);
			}
		}
		if (lItem.mode == 3) {
			if (rItem.mode == 2) {
				putF2(19, rItem.register, lItem.scopeReg,
						lItem.register);
			} else if (rItem.mode == 3) {
				putF2(31, rItem.register, rItem.scopeReg,
						rItem.register);
				putF2(19, rItem.register, lItem.scopeReg,
						lItem.register);
			} else {
				load(rItem);
				putF2(19, rItem.register, lItem.scopeReg,
						lItem.register);
			}
		}
		releaseReg(lItem.register);
		releaseReg(rItem.register);
	}

	public void load(Item loadItem) throws IOException {
		if (loadItem.mode == 1) {
			const2reg(loadItem);

		} else if (loadItem.mode == 0) {
			var2reg(loadItem);

		} else if (loadItem.mode == 3) {
			ref2reg(loadItem);
		}

	}

	public void loadBool(Item loadItem) throws IOException {
		if (loadItem.mode != 4) {
			load(loadItem);
			loadItem.mode = 4;
			loadItem.operator = NOTEQUALS;
			loadItem.fls = 0;
			loadItem.tru = 0;
		}
	}

	public void unloadBool(Item loadItem) throws IOException {
		if (loadItem.mode == 4) { // COND
			cJump(loadItem);
			fixLink(loadItem.tru);
			loadItem.mode = 2;
			putF1(1, loadItem.register, 0, 1);
			putF3(17, 2); // BR unconditional
			fixLink(loadItem.fls);
			putF1(1, loadItem.register, 0, 0);
		}
	}

	public void const2reg(Item constItem) throws IOException {
		constItem.mode = 2;
		constItem.register = requestReg();
		putF1(1, constItem.register, 0, constItem.value);
		constItem.value = 0;
		constItem.offset = 0;
	}

	public void var2reg(Item varItem) throws IOException {
		int tempReg;
		varItem.mode = 2;
		tempReg = requestReg();
		putF1(7, tempReg, varItem.scopeReg, varItem.offset);
		varItem.register = tempReg;
		varItem.offset = 0;
	}

	public void ref2reg(Item refItem) throws IOException {
		refItem.mode = 3;
		putF1(7, refItem.register, refItem.scopeReg, refItem.offset);
		refItem.offset = 0;
	}

	public void parseExpression(Item expItem) throws Exception {
		Item tempItem;
		if (token.symbolId == MINUS) {
			nextToken();
			expItem.mode = 1;
			expItem.iType = integer;
			expItem.value = 0;
			tempItem = new Item();
			parseTerm(tempItem);
			simpleExpBinOp(expItem, tempItem, MINUS);
		} else {
			parseTerm(expItem);
		}
		while (token.symbolId == PLUS || token.symbolId == MINUS
				|| token.symbolId == DOUBLEEQUALS
				|| token.symbolId == NOTEQUALS || token.symbolId == GREATER
				|| token.symbolId == LESS || token.symbolId == GTE
				|| token.symbolId == LTE || token.symbolId == OR) {
			tempItem = new Item();
			if (token.symbolId == PLUS) {
				nextToken();
				parseTerm(tempItem);
				simpleExpBinOp(expItem, tempItem, PLUS);
			} else if (token.symbolId == MINUS) {
				nextToken();
				parseTerm(tempItem);
				simpleExpBinOp(expItem, tempItem, MINUS);
			} else if (token.symbolId == DOUBLEEQUALS) {
				nextToken();
				parseTerm(tempItem);
				expressionOperator(expItem, tempItem, DOUBLEEQUALS);
			} else if (token.symbolId == NOTEQUALS) {
				nextToken();
				parseTerm(tempItem);
				expressionOperator(expItem, tempItem, NOTEQUALS);
			} else if (token.symbolId == GREATER) {
				nextToken();
				parseTerm(tempItem);
				expressionOperator(expItem, tempItem, GREATER);
			} else if (token.symbolId == LESS) {
				nextToken();
				parseTerm(tempItem);
				expressionOperator(expItem, tempItem, LESS);
			} else if (token.symbolId == GTE) {
				nextToken();
				parseTerm(tempItem);
				expressionOperator(expItem, tempItem, GTE);
			} else if (token.symbolId == LTE) {
				nextToken();
				parseTerm(tempItem);
				expressionOperator(expItem, tempItem, LTE);
			} else if (token.symbolId == OR) {
				nextToken();
				simpleExpressionOR(expItem);
				parseTerm(tempItem);
				simpleExpBinOp(expItem, tempItem, OR);
			}
		}
	}

	public void parseTerm(Item termItem) throws Exception {
		Item tempItem;
		parseFactor(termItem);
		while (token.symbolId == TIMES || token.symbolId == SLASH
				|| token.symbolId == AND) {
			tempItem = new Item();
			if (token.symbolId == TIMES) {
				nextToken();
				parseFactor(tempItem);
				termOperator(termItem, tempItem, TIMES);
			}
			if (token.symbolId == SLASH) {
				nextToken();
				parseFactor(tempItem);
				termOperator(termItem, tempItem, SLASH);
			}
			if (token.symbolId == AND) {
				nextToken();
				termAND(termItem);
				parseFactor(tempItem);
				termOperator(termItem, tempItem, AND);
			}
		}
	}

	public void termAND(Item item) throws IOException {
		if (item.iType.form == 3) {// BOOL
			loadBool(item);
			createCondBranch(negateOp(item.operator), item.register, 0,
					item.tru);
			releaseReg(item.register);
			item.fls = PC - 1;
			fixLink(item.tru);
			item.tru = 0;
		} else {
			error("Boolean expression expected @ line " + token.lineNumber);
		}
	}

	public void parseFactor(Item facItem) throws Exception {
		if (token.symbolId == IDENTIFIER && nextToken.symbolId == LEFTPAR) {
			procedureCall(facItem);
		} else if (token.symbolId == IDENTIFIER) {
			if (contains(token.identifier)) {
				facItem.mode = 0;
				facItem.iType = getNode(token.identifier).type;
				facItem.offset = getNode(token.identifier).offset;
				if (containsLocal(token.identifier)) {
					facItem.scopeReg = 29;
				}
				if (procedureContext != null)
					if (procContainsParam(procedureContext, token.identifier))
						facItem.scopeReg = 29;

				selector(facItem);

				// nextToken();

			} else {
				error("Undeclared variable " + token.identifier + " @ line "
						+ token.lineNumber);
			}
		} else if (token.symbolId == NUMBER) {
			facItem.mode = 1;
			facItem.iType = integer;
			facItem.value = token.number;
			nextToken();
		} else if (token.symbolId == LEFTPAR) {
			nextToken();
			parseExpression(facItem);
			if (token.symbolId == RIGHTPAR) {
				nextToken();
			} else {
				errorAtToken(")", token);
			}
		} else if (token.symbolId == BANG) {
			nextToken();
			parseFactor(facItem);
			factorOperator(facItem);
		} else if (token.symbolId == TRUE) {
			facItem.mode = 1; // CONST
			facItem.iType = bool;
			facItem.value = 1;
			nextToken();
		} else if (token.symbolId == FALSE) {
			facItem.mode = 1; // CONST
			facItem.iType = bool;
			facItem.value = 0;
			nextToken();
		} else if (token.symbolId == NULL) {
			facItem.mode = 1; // CONST
			facItem.iType = unknownType;
			facItem.value = 999;
		} else if (token.symbolId == STRING) {

			error("Strings not supported yet.");
		}

	}

	public void factorOperator(Item item) throws IOException {
		int tmp;
		if (item.iType.form == 3) { // BOOL
			loadBool(item);
			tmp = item.fls;
			item.fls = item.tru;
			item.tru = tmp;
			item.operator = negateOp(item.operator);
		} else {
			error("Boolean expression expected @ line " + token.lineNumber);
		}
	}

	public void selector(Item selecItem) throws Exception {
		SymbolNode node;
		int size;
		array = false;
		node = getNode(token.identifier);

		size = node.size;
		nextToken();
		Item indexItem;
		while (token.symbolId == DOT || token.symbolId == LEFTBRACK) {
			// RECORD
			if (token.symbolId == DOT) {
				nextToken();
				if (token.symbolId == IDENTIFIER) {
					node = getField(selecItem.iType, token.identifier);

					if ((node.type.form == 4 || node.type.form == 5)
							&& nextToken.symbolId == EQUALS) {
						parseFieldAllocation(node, selecItem, size);
					} else if (node.type.form < 6) {
						if (!array) {
							if (selecItem.mode == 3) {
								putF2(31, selecItem.register, 28,
										selecItem.register);
							} else {
								load(selecItem);
							}
						}
						putF1(1, selecItem.register,
								selecItem.register, node.offset);
						selecItem.mode = 3;
						selecItem.iType = node.type;
						nextToken();
					}

				} else {
					errorAtToken("identifier", token);
				}
			}
			// /ARRAY
			if (token.symbolId == LEFTBRACK) {
				nextToken();
				indexItem = new Item();
				parseExpression(indexItem);
				if (indexItem.mode == 1) {
					if (!array) {
						if (selecItem.mode == 3) {
							putF2(31, selecItem.register, 28,
									selecItem.register);
						} else {
							load(selecItem);
						}
						putF1(1, selecItem.register,
								selecItem.register, indexItem.value * 4
										* (size / 4));
					} else {
						putF1(1, selecItem.register,
								selecItem.register, indexItem.value * 4
										* (size / 4) * node.type.length);
					}

					selecItem.mode = 3;
					selecItem.iType = node.type.base;
				} else {
					load(indexItem);
					putF1(3, indexItem.register, indexItem.register, 4);
					if (!array) {
						if (selecItem.mode == 3) {
							putF2(31, selecItem.register, 28,
									selecItem.register);
						} else {
							load(selecItem);
						}
					}
					putF2(20, selecItem.register, selecItem.register,
							indexItem.register);
					selecItem.mode = 3;
					releaseReg(indexItem.register);
					selecItem.iType = node.type.base;
					selecItem.iType.field = node.type.base.field;
				}
				nextToken();
				// nextToken();
				array = true;
			}
		}
	}

	private void parseFieldAllocation(SymbolNode node, Item item, int size)
			throws Exception {
		int register;
		nextToken();
		nextToken();
		if (token.symbolId == NEW && node.type.form == 4) {
			nextToken();
			if (token.symbolId < 4) {
				nextToken();
				if (token.symbolId == LEFTBRACK && nextToken.symbolId == NUMBER) {
					if (item.mode == 3) {
						putF2(31, item.register, 28, item.register);
					} else {
						load(item);
					}
					putF1(1, item.register, item.register, node.offset);
					register = requestReg();
					putF1(1, register, 0, heapPointer);
					putF2(19, register, 28, item.register);
					item.iType = node.type;
					heapPointer = nextToken.number * 4 + heapPointer;
					nextToken();
				} else {
					errorAtToken("[", token);
					errorAtToken("number", nextToken);
				}
			} else if (contains(token.identifier)) {
				if (token.symbolId == LEFTBRACK && nextToken.symbolId == NUMBER) {
					if (item.mode == 3) {
						putF2(31, item.register, 28, item.register);
					} else {
						load(item);
					}
					putF1(1, item.register, item.register, node.offset);
					register = requestReg();
					putF1(1, register, 0, heapPointer);
					putF2(19, register, 28, item.register);
					node.type.length = nextToken.number;
					heapPointer = nextToken.number * 4 * size + heapPointer;
					nextToken();
				} else {
					errorAtToken("[", token);
					errorAtToken("number", nextToken);
				}
			}

		} else if (token.symbolId == NEW && node.type.form == 5) {
			nextToken();
			if (contains(token.identifier)) {
				if (item.mode == 3) {
					putF2(31, item.register, 28, item.register);
				} else {
					load(item);
				}
				putF1(1, item.register, item.register, node.offset);
				register = requestReg();
				putF1(1, register, 0, heapPointer);
				putF2(19, register, 28, item.register);
				item.iType = node.type;
				heapPointer = size + heapPointer;
				nextToken();

				if (token.symbolId == LEFTPAR) {

				}
			}
		} else {
			errorAtToken("new", token);
			errorAtToken("valid type (boolean, char, int, String)", nextToken);
		}
	}

	public void termOperator(Item leftItem, Item rightItem, int opSymbolId)
			throws IOException {
		if (opSymbolId == AND) {
			if (leftItem.iType.form == 3 && rightItem.iType.form == 3) {
				loadBool(rightItem);
				leftItem.register = rightItem.register;
				leftItem.fls = concatenate(rightItem.fls, leftItem.fls);
				leftItem.tru = rightItem.tru;
				leftItem.operator = rightItem.operator;
			} else {
				error("Boolean expressions expected @ line " + token.lineNumber);
			}
		} else if (rightItem.mode == 1) {
			if (leftItem.mode == 1) {
				if (opSymbolId == TIMES) {
					leftItem.value = leftItem.value * rightItem.value;
				} else if (opSymbolId == SLASH) {
					leftItem.value = leftItem.value / rightItem.value;
				}
				if (opSymbolId == TIMES) {
					putF1(3, leftItem.register, leftItem.register,
							rightItem.value);
				} else if (opSymbolId == SLASH) {
					putF1(4, leftItem.register, leftItem.register,
							rightItem.value);
				}
			} else {
				load(rightItem);
				if (leftItem.mode == 3) {
					putF2(31, leftItem.register, leftItem.scopeReg,
							leftItem.register);
					leftItem.mode = 2;
				}
				if (leftItem.mode == 0) {
					load(leftItem);
				}
				if (opSymbolId == TIMES) {
					putF2(22, leftItem.register, leftItem.register,
							rightItem.register);
				} else if (opSymbolId == SLASH) {
					putF2(23, leftItem.register, leftItem.register,
							rightItem.register);
				}
			}
		} else if (rightItem.mode == 0 || rightItem.mode == 3) {
			if (leftItem.mode == 0 || leftItem.mode == 1) {
				load(leftItem);
			}
			if (rightItem.mode == 0 || rightItem.mode == 1) {
				load(rightItem);
			}
			if (rightItem.mode == 3) {
				putF2(31, rightItem.register, rightItem.scopeReg,
						rightItem.register);
				rightItem.mode = 2;
			}
			if (leftItem.mode == 3) {
				putF2(31, leftItem.register, leftItem.scopeReg,
						leftItem.register);
				leftItem.mode = 2;
			}
			if (opSymbolId == TIMES) {
				putF2(22, leftItem.register, leftItem.register,
						rightItem.register);
			} else if (opSymbolId == SLASH) {
				putF2(23, leftItem.register, leftItem.register,
						rightItem.register);
			}
		}
		releaseReg(rightItem.register);
	}

	public void simpleExpBinOp(Item leftItem, Item rightItem, int opSymbolId)
			throws IOException {
		if (opSymbolId == OR) {
			if (leftItem.iType.form == 3 && rightItem.iType.form == 3) {
				loadBool(rightItem);
				leftItem.register = rightItem.register;
				leftItem.fls = rightItem.fls;
				leftItem.tru = concatenate(rightItem.tru, leftItem.tru);
				leftItem.operator = rightItem.operator;
			} else {
				error("Boolean expressions expected @ line " + token.symbolId);
			}
		} else if (rightItem.mode == 1) {
			if (leftItem.mode == 1) {
				if (opSymbolId == PLUS) {
					leftItem.value = leftItem.value + rightItem.value;
				} else if (opSymbolId == MINUS) {
					leftItem.value = leftItem.value - rightItem.value;
				}
				// if (opSymbolId == PLUS) {
				// putF1(1, leftItem.register, leftItem.register,
				// rightItem.value);
				// } else if (opSymbolId == MINUS) {
				// putF1(2, leftItem.register, leftItem.register,
				// rightItem.value);
				// }
			} else {
				load(rightItem);
				if (leftItem.mode == 3) {
					putF2(31, leftItem.register, leftItem.scopeReg,
							leftItem.register);
					leftItem.mode = 2;
				}
				if (leftItem.mode == 0) {
					load(leftItem);
				}
				if (opSymbolId == PLUS) {
					putF2(20, leftItem.register, leftItem.register,
							rightItem.register);
				} else if (opSymbolId == MINUS) {
					putF2(21, leftItem.register, leftItem.register,
							rightItem.register);
				}
			}
		} else if (rightItem.mode == 0 || rightItem.mode == 3) {
			if (leftItem.mode == 0 || leftItem.mode == 1) {
				load(leftItem);
			}
			if (rightItem.mode == 0 || rightItem.mode == 1) {
				load(rightItem);
			}
			if (rightItem.mode == 3) {
				putF2(31, rightItem.register, rightItem.scopeReg,
						rightItem.register);
				rightItem.mode = 2;
			}
			if (leftItem.mode == 3) {
				putF2(31, leftItem.register, leftItem.scopeReg,
						leftItem.register);
				leftItem.mode = 2;
			}
			if (opSymbolId == PLUS) {
				putF2(20, leftItem.register, leftItem.register,
						rightItem.register);
			} else if (opSymbolId == MINUS) {
				putF2(21, leftItem.register, leftItem.register,
						rightItem.register);
			}
		} else if (rightItem.mode == 2) {
			System.out.println("/////////////////////////////////////////");
			System.out.println("Now critical: modes = " + leftItem.mode + ", "
					+ rightItem.mode);
			System.out.println("/////////////////////////////////////////");
			if (leftItem.mode == 0 || leftItem.mode == 1) {
				load(leftItem);
			}
			if (opSymbolId == PLUS) {
				putF2(20, leftItem.register, leftItem.register,
						rightItem.register);
			} else if (opSymbolId == MINUS) {
				putF2(21, leftItem.register, leftItem.register,
						rightItem.register);
			}
			releaseReg(leftItem.register);
		}
		releaseReg(rightItem.register);
	}

	public int concatenate(int head1, int head2) {
		int head1Backup;
		head1Backup = head1;
		// warning("Experimental concatenate implementation.");
		while (head1 != 0) {
			head1 = decodeC(head1);
		}
		encodeC(head2, head1);
		// System.out.println("Concat values: head1: " + head1 + ", head2: "
		// + head2 + ", head1Backup: " + head1Backup);
		// System.out.println("---------------List: ");
		// int i;
		// i = head2;
		// while (i != 0) {
		// System.out.println("Current c: " + decodeC(i));
		// i = decodeC(i);
		// }
		return head2;
	}

	public void simpleExpressionOR(Item item) throws IOException {
		if (item.iType.form == 3) {// BOOL
			loadBool(item);
			createCondBranch(item.operator, item.register, 0, item.tru);
			releaseReg(item.register);
			item.tru = PC - 1;
			fixLink(item.fls);
			item.fls = 0;
		} else {
			error("Boolean expression expected @ line " + token.lineNumber);
		}
	}

	public void expressionOperator(Item leftItem, Item rightItem, int operator)
			throws IOException { // comparison
		if (leftItem.mode == 3) {
			putF2(31, leftItem.register, leftItem.scopeReg,
					leftItem.register);
		} else {
			load(leftItem);
		}
		if (rightItem.mode != 1 || rightItem.value != 0) {
			if (rightItem.mode == 3) {
				putF2(31, rightItem.register, rightItem.scopeReg,
						rightItem.register);
			} else {
				load(rightItem);
			}
			// create CMP instruction
			putF2(25, leftItem.register, leftItem.register,
					rightItem.register);
			releaseReg(rightItem.register);
		}
		leftItem.mode = 4; // condition mode
		leftItem.iType = bool;
		leftItem.operator = operator;
		leftItem.fls = 0;
		leftItem.tru = 0;
		// } else {
		// error("Integer expressions expected @ line " + token.lineNumber);
		// }
	}

	public void parseWhileStatement() throws Exception {
		Item item;
		int bJumpAddress;

		if (token.symbolId == WHILE) {
			nextToken();
		} else {
			errorAtToken("while", token);
		}
		if (token.symbolId == LEFTPAR) {
			nextToken();
		} else {
			errorAtToken("(", token);
		}

		bJumpAddress = PC;

		item = new Item();
		parseExpression(item);

		if (item.iType.form == 3) {
			loadBool(item);
			cJump(item);
			fixLink(item.tru);
		} else {
			error("Boolean expression expected @ line " + token.lineNumber);
		}

		if (token.symbolId == RIGHTPAR) {
			nextToken();
		} else {
			errorAtToken(")", token);
		}

		if (token.symbolId == LEFTCURL) {
			nextToken();
			parseStatementSequence();

			nextToken();
		} else {
			parseStatement();

		}

		bJump(bJumpAddress);
		fixLink(item.fls);
	}

	public void parseIfStatement() throws Exception {
		Item item;
		int fJumpAddress;

		if (token.symbolId == IF) {
			nextToken();
		} else {
			errorAtToken("if", token);
		}
		if (token.symbolId == LEFTPAR) {
			nextToken();
		} else {
			errorAtToken("(", token);
		}
		item = new Item();
		parseExpression(item);
		if (item.iType.form == 3) {
			loadBool(item);
			cJump(item);
			fixLink(item.tru);

		} else {
			error("Boolean expression expected @ line " + token.lineNumber);
		}
		if (token.symbolId == RIGHTPAR) {
			nextToken();
		} else {
			errorAtToken(")", token);
		}

		if (token.symbolId == LEFTCURL) {
			nextToken();
			parseStatementSequence();

			nextToken();
		} else {
			parseStatement();

		}
		if (token.symbolId == ELSE) {
			nextToken();
			fJumpAddress = fJump();
			fixLink(item.fls);

			if (token.symbolId == LEFTCURL) {
				nextToken();
				parseStatementSequence();
				nextToken();
			} else {
				parseStatement();

			}
			fixUp(fJumpAddress);
		} else {
			fixLink(item.fls);
		}
	}

	public void bJump(int backAddress) throws IOException {
		putF3(17, backAddress - PC); // BR unconditional
	}

	public void cJump(Item item) throws IOException { // conditional jump
		int operator;
		operator = negateOp(item.operator);
		createCondBranch(operator, item.register, 0, item.fls);
		releaseReg(item.register);
		item.fls = PC - 1;
	}

	public void fixUp(int branchAddress) {
		encodeC(branchAddress, PC - branchAddress);
	}

	public void fixLink(int branchAddress) {
		int nextBranchAddress;
		while (branchAddress != 0) {
			nextBranchAddress = decodeC(branchAddress);
			fixUp(branchAddress);
			branchAddress = nextBranchAddress;
		}
	}

	// creates conditional branch instruction based on the operator
	public void createCondBranch(int operator, int register, int offset,
			int address) throws IOException {
		if (operator == DOUBLEEQUALS) {
			putF1(11, register, 0, address); // BEQ
		} else if (operator == NOTEQUALS) {
			putF1(16, register, 0, address); // BNE
		} else if (operator == GREATER) {
			putF1(13, register, 0, address); // BGT
		} else if (operator == LESS) {
			putF1(15, register, 0, address); // BLT
		} else if (operator == GTE) {
			putF1(12, register, 0, address); // BGE
		} else if (operator == LTE) {
			putF1(14, register, 0, address); // BLE

		} else {
			error("Cannot create branch for unknown operator: " + operator);
		}
	}

	public int fJump() throws IOException { // forward jump
		putF3(17, 0); // BR unconditional
		return PC - 1;
	}

	public int negateOp(int operator) {
		if (operator == DOUBLEEQUALS) {
			return NOTEQUALS;
		} else if (operator == NOTEQUALS) {
			return DOUBLEEQUALS;
		} else if (operator == GREATER) {
			return LTE;
		} else if (operator == LESS) {
			return GTE;
		} else if (operator == GTE) {
			return LESS;
		} else if (operator == LTE) {
			return GREATER;
		} else {
			error("Received unknown operator to negate: " + operator);
			return -1;
		}
	}

	public void parseMain() throws Exception {
		nextToken();
		nextToken();
		if (token.symbolId == VOID && nextToken.symbolId == IDENTIFIER) {
			nextToken();
			nextToken();
		}
		if (token.symbolId == LEFTPAR && nextToken.symbolId == STRING) {
			nextToken();
			nextToken();
		}
		if (token.symbolId == LEFTBRACK && nextToken.symbolId == RIGHTBRACK) {
			nextToken();
			nextToken();
		}
		if (token.symbolId == IDENTIFIER && nextToken.symbolId == RIGHTPAR) {
			nextToken();
			nextToken();
		}
		if (token.symbolId == LEFTCURL) {
			nextToken();
		}
		parseStatementSequence();

	}

	public void nextToken() throws Exception {
		token.identifier = nextToken.identifier;
		token.symbolId = nextToken.symbolId;
		token.number = nextToken.number;
		token.lineNumber = nextToken.lineNumber;
		// System.out.println("TOKEN: " + token.identifier + ", " +
		// token.symbolId
		// + ", " + token.number + ", " + token.lineNumber);
		getToken();
	}

	public void errorAtToken(String expected, Token unexpected) {
		System.out
				.println("+-----------------------------[ E R R O R ]-----------------------------");
		if (unexpected.symbolId == 101) {
			System.out.println("| " + "Unexpected token [NUMBER: "
					+ unexpected.number + "] received @ line "
					+ unexpected.lineNumber);
		} else if (unexpected.symbolId == 102) {
			System.out.println("| " + "Unexpected token [IDENTIFIER: "
					+ unexpected.identifier + "] received @ line "
					+ unexpected.lineNumber);
		} else {
			System.out.println("| " + "Unexpected token received @ line "
					+ unexpected.lineNumber);
			System.out.println("| " + "Details:");
			System.out.println("| " + "\ttoken.identifier = "
					+ unexpected.identifier);
			System.out.println("| " + "\ttoken.symbolId = "
					+ unexpected.symbolId);
			System.out.println("| " + "\ttoken.number = " + unexpected.number);
		}
		System.out.println("| " + "Expected: " + expected);
		System.out
				.println("+-----------------------------------------------------------------------\n");
		numOfErrors = numOfErrors + 1;
	}

	public void error(String message) {
		System.out
				.println("+-----------------------------[ E R R O R ]-----------------------------");
		System.out.println("| " + message);
		System.out
				.println("+-----------------------------------------------------------------------\n");
		numOfErrors = numOfErrors + 1;
	}

	public void warning(String message) {
		System.out
				.println("+-----------------------------[ W A R N I N G ]-----------------------------");
		System.out.println("| " + message);
		System.out
				.println("+---------------------------------------------------------------------------\n");
	}

	public void procedureImplementation() throws Exception {
		Item item;
		SymbolNode node;
		item = new Item();
		node = new SymbolNode();
		nextToken(); // now token is a type
		returnType(item);
		if (token.symbolId == IDENTIFIER) {
			node = getNode(token.identifier);
			if (node != null) {
				if (node.type.form != item.iType.form) {
					warning("Return type mismatch in procedure");
				}
				// To do:
				// fixLink(node.offset);
				// (if procedure has already been implemented)

			} else {
				addMethod(token.identifier, item.iType);
				node = getNode(token.identifier);
			}

			node.offset = PC;
			nextToken();
			formalParameters(node);
		} else {
			errorAtToken("Identifier", token);
		}
		returnFJumpAddress = 0;

		if (token.symbolId == LEFTCURL) {
			nextToken();
		} else {
			errorAtToken("{", token);
		}

		prologue(variableDeclarationSequence(node) * 4);
		procedureContext = node;
		parseStatementSequence();
		fixLink(returnFJumpAddress);
		epilogue(node.size * 4, node);
		resetLocalSymbolTable();
		if (token.symbolId == RIGHTCURL) {
			nextToken();
		} else {
			error("Missing }");
		}
	}

	public int variableDeclarationSequence(SymbolNode node) throws Exception {
		int count;
		count = 0;
		while (token.symbolId == INT || token.symbolId == CHAR
				|| token.symbolId == BOOLEAN) {
			Type type;
			type = basicArrayRecordType();
			if (token.symbolId == IDENTIFIER) {
				SymbolNode newLocal;
				newLocal = getNode(token.identifier);
				if (newLocal != null) {
					error("Found duplicate variable '" + token.identifier + "'");
				} else {
					newLocal = new SymbolNode();
					newLocal.name = token.identifier;
					newLocal.type = type;
					addLocalNode(newLocal);
					nextToken();
					nextToken();
				}
			} else {
				errorAtToken("Identifier", token);
			}
			count = count + 1;
		}
		return count;
	}

	public void prologue(int localSize) throws IOException {
		putF2(10, 31, 30, 4); // PSH
		putF2(10, 29, 30, 4);
		putF2(20, 29, 0, 30);
		putF1(2, 30, 30, localSize); // SUBI
	}

	public void epilogue(int paramSize, SymbolNode proc) throws IOException {
		putF2(20, 30, 0, 29); // ADD
		putF2(9, 29, 30, 4); // POP
		putF2(9, 31, 30, paramSize + 4);
		if (!proc.name.equals("main")) {
			putF2(26, 0, 0, 31); // RET
		} else {
			putF3(17, 1); // JMP (if main)
		}
	}

	public void formalParameters(SymbolNode node) throws Exception {
		// System.out.println("---------------FORMAL PARAMETERS--------------");
		int numberOfParameters;
		SymbolNode nextParameter;
		numberOfParameters = 0;

		if (token.symbolId == LEFTPAR) {
			nextToken();
		} else {
			errorAtToken("(", token);
		}

		nextParameter = node.params;

		if (token.symbolId == INT || token.symbolId == CHAR
				|| token.symbolId == BOOLEAN || token.symbolId == IDENTIFIER
				|| contains(token.identifier) || token.symbolId == STRING) {
			nextParameter = formalParameter(node, nextParameter);
			numberOfParameters = numberOfParameters + 1;

			while (token.symbolId == COMMA) {
				nextToken();
				nextParameter = formalParameter(node, nextParameter);
				numberOfParameters = numberOfParameters + 1;
			}
		}
		node.size = numberOfParameters;
		nextParameter = node.params;
		while (nextParameter != null) {
			numberOfParameters = numberOfParameters - 1;
			nextParameter.offset = numberOfParameters * 4 + 8;
			nextParameter = nextParameter.next;
		}

		if (token.symbolId == RIGHTPAR) {
			nextToken();
		} else {
			errorAtToken("}", token);
		}

	}

	public SymbolNode formalParameter(SymbolNode node,
			SymbolNode formalParameter) throws Exception {
		Type type;
		type = basicArrayRecordType();

		if (token.symbolId == IDENTIFIER) {
			if (formalParameter != null) {
				if (type != formalParameter.type) {
					warning("Type mismatch in procedure declaration and call");
				}
				// check if works properly with our implementation
				if (getField(formalParameter.type, token.identifier) != null) {
					error("Parameter name already used: " + token.identifier);
				}
				formalParameter.name = token.identifier;
			} else {
				formalParameter = createFormalParameter(node, type,
						token.identifier);
			}
			nextToken();
			formalParameter = formalParameter.next;
		} else {
			errorAtToken("Identifier", token);
		}

		return formalParameter;
	}

	public SymbolNode getProcParameter(SymbolNode proc, String name) {
		tempNode = null;
		tempNode = proc.params;
		if (tempNode != null) {
			if (tempNode.name.equals(name))
				return tempNode;
			while (tempNode.next != null) {
				if (tempNode.next.name.equals(name))
					return tempNode.next;
				tempNode = tempNode.next;
			}
			return null;
		}
		return tempNode;
	}

	public SymbolNode createFormalParameter(SymbolNode node, Type formalType,
			String formalIdent) {
		SymbolNode paramBackup;

		if (node.params == null) {
			node.params = new SymbolNode();
			node.params.name = formalIdent;
			node.params.type = formalType;
			return node.params;
		} else {
			paramBackup = node.params;
			while (node.params.next != null) {
				node.params = node.params.next;
			}
			node.params.next = new SymbolNode();
			node.params.next.name = formalIdent;
			node.params.next.type = formalType;
			node.params = paramBackup;
		}
		return node.params;
	}

	public void procedureReturn() throws Exception {
		Item item;

		if (token.symbolId == RETURN) {
			nextToken();
		} else {
			errorAtToken("return", token);
		}

		if (token.symbolId == PLUS || token.symbolId == MINUS
				|| token.symbolId == IDENTIFIER || token.symbolId == INT
				|| token.symbolId == LEFTPAR || token.symbolId == BANG
				|| token.symbolId == STRING || token.symbolId == NUMBER
				|| token.symbolId == TRUE || token.symbolId == FALSE
				|| token.symbolId == NULL) {
			item = new Item();
			parseExpression(item);
			if (item.iType != unknownType
					&& item.iType != procedureContext.type) {
				warning("Return type mismatch @ line " + token.lineNumber);
			}

			if (item.iType == bool) {
				unloadBool(item);
			}

			load(item);
			putF2(20, 27, 0, item.register);
			releaseReg(item.register);
		}
		returnFJumpAddress = fJumpChain(returnFJumpAddress);
		nextToken();
	}

	public int fJumpChain(int branchAddress) throws IOException {
		putF3(17, branchAddress);
		return PC - 1;
	}

	public Type basicArrayRecordType() throws Exception {
		Type type;
		type = new Type();
		if (token.symbolId == INT) {
			type = integer;
		} else if (token.symbolId == CHAR) {
			type = character;
		} else if (token.symbolId == STRING) {
			type = string;
		} else if (token.symbolId == BOOLEAN) {
			type = bool;
		} else if (token.symbolId == VOID) {
			type = vd;
		}
		// doesnt work properly yet
		// else if (getNode(token.identifier).type.form == 4) {
		// type = getNode(token.identifier).type;
		// } else if (getNode(token.identifier).type.form == 5) {
		// type = getNode(token.identifier).type;
		// }
		nextToken();
		return type;
	}

	public void returnType(Item item) throws Exception {

		if (token.symbolId == STATIC) {
			nextToken();
		}
		if (token.symbolId == INT) {
			item.iType = integer;
		} else if (token.symbolId == CHAR) {
			item.iType = character;
		} else if (token.symbolId == STRING) {
			item.iType = string;
		} else if (token.symbolId == BOOLEAN) {
			item.iType = bool;
		} else if (token.symbolId == VOID) {
			item.iType = vd;
		} else if (getNode(token.identifier).type.form == 4) {
			item.iType = getNode(token.identifier).type;
		} else if (getNode(token.identifier).type.form == 5) {
			item.iType = getNode(token.identifier).type;
		}
		nextToken();
	}

	public void procedureCall(Item item) throws Exception {
		SymbolNode node;
		int numOfRegisters;
		node = getNode(token.identifier);
		if (node == null) {
			warning("Undeclared procedure: " + token.identifier);
			node = new SymbolNode();
			node.nodeClass = 2;
			node.type = unknownType;
			node.offset = 0;
			addNode(node);
		} else {
			if (node.nodeClass == 2) {
				item.mode = 2;
				item.iType = node.type; // type of return value
				numOfRegisters = pushUsedRegisters();
				nextToken();
				actualParameters(node);

				// if ((node.offset != 0) && !isBSR(node.offset)) {
				// sJump(node.offset - PC);
				// } else {
				// node.offset = sJump(node.offset); //@ + 1: first instruction
				// is the JMP to main
				// }

				// replace the following line with the previous lines
				sJump(node.offset - PC);

				popUsedRegisters(numOfRegisters);
				item.register = requestReg();
				putF2(20, item.register, 0, 27); // BSR (where the
															// current PC gets
															// stored into LINK
															// reg[31]
			} else {
				error("Procedure name is a variable! At the moment a method cant have the same name as a variable.");
			}
		}

	}

	public void popUsedRegisters(int numOfRegisters) throws IOException {
		int reg;
		while (numOfRegisters > 0) {
			reg = requestReg();
			putF2(9, reg, 30, 4); // POP
		}
	}

	public int sJump(int branchAddress) throws IOException {
		putF3(18, branchAddress);
		return PC - 1;
	}

	public boolean isBSR(int offset) {
		error("Calling unimplemented procedure isBSR");
		return false;
	}

	public void actualParameters(SymbolNode node) throws Exception {
		SymbolNode nextFormalParameter;
		Item item;

		if (token.symbolId == LEFTPAR) {
			nextToken();
		} else {
			errorAtToken("(", token);
		}

		nextFormalParameter = node.params;
		if (token.symbolId == PLUS || token.symbolId == MINUS
				|| token.symbolId == IDENTIFIER || token.symbolId == NUMBER
				|| token.symbolId == LEFTPAR || token.symbolId == BANG
				|| token.symbolId == STRING || token.symbolId == FALSE
				|| token.symbolId == TRUE) {
			nextFormalParameter = actualParameter(node, nextFormalParameter);

			while (token.symbolId == COMMA) {
				nextToken();
				nextFormalParameter = actualParameter(node, nextFormalParameter);
			}
		}

		while (nextFormalParameter != null) {
			warning("Actual parameter expected");
			item = new Item();
			item.mode = 1; // CONST
			item.iType = integer;
			item.value = 0;
			pushParameter(item);
			nextFormalParameter = nextFormalParameter.next;
		}

		if (token.symbolId == RIGHTPAR) {
			nextToken();
		} else {
			errorAtToken(")", token);
		}
	}

	public SymbolNode actualParameter(SymbolNode node,
			SymbolNode formalParameter) throws Exception {
		Item item;

		if (token.symbolId == PLUS || token.symbolId == MINUS
				|| token.symbolId == IDENTIFIER || token.symbolId == NUMBER
				|| token.symbolId == LEFTPAR || token.symbolId == BANG
				|| token.symbolId == STRING || token.symbolId == FALSE
				|| token.symbolId == TRUE) {
			item = new Item();
			parseExpression(item);

			if (formalParameter != null) {
				if (item.iType != formalParameter.type) {
					error("Type mismatch in procedure call @ line "
							+ token.lineNumber);
				}
			} else {
				formalParameter = createAnonymousParameter(node, item.iType);
			}

			pushParameter(item);
			formalParameter = formalParameter.next;

		} else {
			error("Actual parameter expected @ line " + token.lineNumber);
		}
		return formalParameter;
	}

	public void pushParameter(Item item) throws IOException {
		// System.out.println("---- push: mode = " + item.mode + ", value: "
		// + item.value);
		if (item.iType == bool) {
			unloadBool(item);
		}
		load(item);
		putF2(10, item.register, 30, 4); // PSH
		releaseReg(item.register);
	}

	public SymbolNode createAnonymousParameter(SymbolNode node, Type type) {
		warning("Experimental createAnonymousParameter implementation");
		SymbolNode newParam;
		newParam = createFormalParameter(node, type, null); // new formal
															// parameter without
															// a name!
		return newParam;
	}

	/*
	 * @ return returns number of registers pushed, needed for later
	 * popUsedRegisters
	 */
	public int pushUsedRegisters() throws IOException {
		int i;
		int numRegs;
		boolean skip;
		skip = false;
		numRegs = 0;
		i = 26; // reverse order, so the pop order is correct
		while ((i > 0) && !skip) {
			if (regs[i] == 1) {
				putF2(10, i, 30, 4); // PSH
				regs[i] = 0;
				numRegs = numRegs + 1;
			} else {
				skip = true;
			}
			i = i - 1;
		}
		return numRegs;
	}

	/**
	 * @return integer Indicates the number of errors occurred during compiling.
	 * @throws Exception
	 */
	public int compileSourceFile(String fileName) {
		try {
			tempMain = new SymbolNode();
			setScanner(fileName);
			setParser();
			parseClass();
			writeCode();
		} catch (Exception e) {
			error("An error occured @ line " + token.lineNumber);
//			e.printStackTrace();
		}
		return numOfErrors;
	}

	/*************************************************************************************************
	 ******************************** the symbol table of the compiler *******************************
	 *************************************************************************************************/

	public void setSymbolTable() {
		root = new SymbolNode();
		root.name = "root";
		root.nodeClass = 99;
		root.next = null;
		root.type = null;

		localRoot = new SymbolNode();
		localRoot.name = "localRoot";
		localRoot.nodeClass = 99;
		localRoot.next = null;
		localRoot.type = null;

		integer = new Type();
		integer.form = 0;

		character = new Type();
		character.form = 1;

		string = new Type();
		string.form = 2;

		bool = new Type();
		bool.form = 3;

		record = new Type();
		record.form = 5;

		vd = new Type();
		vd.form = 6;

		unknownType = new Type();
		unknownType.form = 7;
	}

	public void addInteger(String name) {
		tempNode = new SymbolNode();
		tempNode.name = name;
		tempNode.offset = offsetCounter;
		offsetCounter = offsetCounter - 4;
		tempNode.type = integer;
		tempNode.nodeClass = 0;
		addNode(tempNode);
	}

	public void addCharacter(String name) {
		tempNode = new SymbolNode();
		tempNode.name = name;
		tempNode.offset = offsetCounter;
		offsetCounter = offsetCounter - 4;
		tempNode.type = character;
		tempNode.nodeClass = 0;
		addNode(tempNode);
	}

	public void addString(String name) {
		tempNode = new SymbolNode();
		tempNode.name = name;
		tempNode.type = string;
		tempNode.nodeClass = 0;
		addNode(tempNode);
	}

	public void addBoolean(String name) {
		tempNode = new SymbolNode();
		tempNode.name = name;
		tempNode.offset = offsetCounter;
		offsetCounter = offsetCounter - 4;
		tempNode.type = bool;
		tempNode.nodeClass = 0;
		addNode(tempNode);
	}

	public void addArray(String name, Type base, int off) {
		tempType = new Type();
		tempType.form = 4;
		tempType.base = base;
		tempNode = new SymbolNode();
		tempNode.name = name;
		tempNode.offset = offsetCounter;
		offsetCounter = offsetCounter + off;
		if (base.form == 5) {
			tempNode.size = 9;
		} else {
			tempNode.size = 4;
		}
		tempNode.type = tempType;
		tempNode.nodeClass = 0;
		addNode(tempNode);
	}

	public void addArray(String name, Type base, int off, int size) {
		tempType = new Type();
		tempType.form = 4;
		tempType.base = base;
		tempNode = new SymbolNode();
		tempNode.name = name;
		tempNode.offset = offsetCounter;
		offsetCounter = offsetCounter + off;
		tempNode.size = size;
		tempNode.type = tempType;
		tempNode.nodeClass = 0;
		addNode(tempNode);
	}

	public void addRecType(String name) {

		SymbolNode tempNode = new SymbolNode();
		tempNode.name = name;
		tempNode.type = new Type();
		tempNode.type.form = 5;

		tempNode.type.field = new SymbolNode();
		tempNode.type.field.name = "start";
		tempNode.type.field.next = null;
		tempNode.nodeClass = 1;
		addNode(tempNode);
	}

	public void addRecord(String name, String type) {
		Type tempType = new Type();
		int size;
		SymbolNode tempNode;
		tempNode = root;
		while (!tempNode.name.equals(type)) {
			tempNode = tempNode.next;
		}
		tempType = tempNode.type;
		size = tempNode.size;
		tempNode = root;
		while (tempNode.next != null) {
			tempNode = tempNode.next;
		}
		tempNode.next = new SymbolNode();
		tempNode.next.name = name;
		tempNode.next.offset = offsetCounter;
		tempNode.next.size = size;
		offsetCounter = offsetCounter - 4;
		tempNode.next.type = tempType;
		tempNode.next.nodeClass = 0;
	}

	public void addMethod(String name, Type type) {
		SymbolNode tempNode = new SymbolNode();
		tempNode.name = name;
		tempNode.type = type;
		tempNode.type.field = new SymbolNode();
		tempNode.type.field.name = "start";
		tempNode.type.field.next = null;
		tempNode.nodeClass = 2;
		addNode(tempNode);
	}

	public void addField(String nodeName, String fieldName, Type type,
			String typename, int off) {
		SymbolNode tempNode;
		fieldNode = getNode(nodeName).type.field;

		if (!typename.equals("")) {
			tempType = getNode(typename).type;
			tempNode = new SymbolNode();
			tempNode.offset = getNode(nodeName).size;
			tempNode.name = fieldName;
			tempNode.type = tempType;
			tempNode.next = null;
		} else {
			tempNode = new SymbolNode();
			tempNode.offset = getNode(nodeName).size;
			tempNode.name = fieldName;
			tempNode.type = type;
			tempNode.next = null;
		}
		getNode(nodeName).size = getNode(nodeName).size + off;
		if (fieldNode.next == null) {
			fieldNode.next = tempNode;
		} else {
			while (fieldNode.next != null) {
				fieldNode = fieldNode.next;
			}
			fieldNode.next = tempNode;
		}
	}

	public SymbolNode getField(Type type, String fieldName) {
		fieldNode = type.field;

		while (!fieldNode.name.equals(fieldName) && fieldNode.next != null) {
			fieldNode = fieldNode.next;
		}

		if (fieldNode.name.equals(fieldName)) {
			return fieldNode;
		}
		// return new SymbolNode();
		return null;
	}

	public void addNode(SymbolNode node) {
		tempNode = root;
		if (tempNode.next == null) {
			root.next = node;
		} else {
			while (tempNode.next != null) {
				tempNode = tempNode.next;
			}
			node.scope = "global";
			tempNode.next = node;
		}
	}

	public void addLocalNode(SymbolNode node) {
		// System.out
		// .println("addLocalNode: " + node.name + ", " + node.type.form);
		localTempNode = localRoot;
		if (localTempNode.next == null) {
			localRoot.next = node;
		} else {
			while (localTempNode.next != null) {
				localTempNode = localTempNode.next;
			}
			if (procedureContext != null)
				node.scope = procedureContext.name;
			localTempNode.next = node;
		}
		node.offset = localOffsetCounter;
		localOffsetCounter = localOffsetCounter - 4;
	}

	public SymbolNode getNode(String name) {
		if (procedureContext != null) {
			tempNode = getProcParameter(procedureContext, name);
			if (tempNode != null) {
				// System.out
				// .println("_:_:_:_:_:_:_:_:getProcParameter returned: "
				// + tempNode.name + ", offset: "
				// + tempNode.offset);
				return tempNode;
			}
		}
		tempNode = getLocalNode(name);
		if (tempNode != null)
			return tempNode;
		tempNode = null;
		tempNode = root;
		while (tempNode.next != null && !tempNode.name.equals(name)) {
			tempNode = tempNode.next;
		}
		if (!tempNode.name.equals(name)) {
			return null;
			// return new SymbolNode();
			// achtung eventuell fehler
		}
		return tempNode;
	}

	public SymbolNode getLocalNode(String name) {
		localTempNode = null;
		localTempNode = localRoot;
		while (localTempNode.next != null && !localTempNode.name.equals(name)) {
			localTempNode = localTempNode.next;
		}
		if (!localTempNode.name.equals(name)) {
			return null;
			// return new SymbolNode();
			// achtung eventuell fehler
		}
		// System.out.println("getLocalNode returns: " + localTempNode.name);
		return localTempNode;
	}

	public boolean contains(String name) {
		if (procedureContext != null) {
			if (procContainsParam(procedureContext, name))
				return true;
		}
		if (containsLocal(name))
			return true;
		tempNode = new SymbolNode();
		tempNode = root;
		while (tempNode.next != null && !tempNode.name.equals(name)) {
			tempNode = tempNode.next;
		}
		if (!tempNode.name.equals(name)) {
			return false;
		}
		return true;

	}

	public boolean procContainsParam(SymbolNode proc, String name) {
		tempNode = null;
		tempNode = proc.params;
		if (tempNode != null) {
			if (tempNode.name.equals(name))
				return true;
			while (tempNode.next != null) {
				if (tempNode.next.name.equals(name))
					return true;
				tempNode = tempNode.next;
			}
		}
		return false;
	}

	public boolean containsLocal(String name) {
		localTempNode = new SymbolNode();
		localTempNode = localRoot;
		while (localTempNode.next != null && !localTempNode.name.equals(name)) {
			localTempNode = localTempNode.next;
		}
		if (!localTempNode.name.equals(name)) {
			return false;
		}
		return true;
	}

	public void resetLocalSymbolTable() {
		localRoot = new SymbolNode();
		localRoot.name = "localRoot";
		localRoot.nodeClass = 99;
		localRoot.next = null;
		localRoot.type = null;
	}

	/*************************************************************************************************
	 **************************************** the main method **************************************** takes argument
	 * for filename and compiles the file with the parser and scanner methods
	 * ******************************************************************************************************/

	public static void compile(String sourceFile) {

		System.out.println("Compiling testfile \"" + sourceFile + "\"...\n");
		CC0815 compiler = new CC0815();
		int errors = compiler.compileSourceFile(sourceFile);
		System.out.println("--------------------------------------");
		System.out.println("Finished with " + errors + " error(s).\n");
	}

	public static void main(String[] args) throws Exception {
		compile(args[0]);
	}
}
