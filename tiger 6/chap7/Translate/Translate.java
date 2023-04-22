package Translate;
import Symbol.Symbol;
import Tree.BINOP;
import Tree.CJUMP;
import Temp.Temp;
import Temp.Label;

//checks (tests work but there are discrepencies): while, for, test12.tig and forTest
//checks (tests work but there are discrepencies): fieldvar and recordexp, test3.tig

// dont have tests to test, but they are implemented: subscript, seqexp

//test 4: causes slight variation



  //typeDec
  //functionDec
  //call working with them

  //IfThenElseExp to remove unecessary JUMPs






public class Translate {
  public Frame.Frame frame;
  public Translate(Frame.Frame f) {
    frame = f;
  }
  private Frag frags;

  //generating the intermediate code for a function
  public void procEntryExit(Level level, Exp body) {
    Frame.Frame myframe = level.frame;
    Tree.Exp bodyExp = body.unEx();
    Tree.Stm bodyStm;
    if (bodyExp != null)
      bodyStm = MOVE(TEMP(myframe.RV()), bodyExp);
    else
      bodyStm = body.unNx();
    ProcFrag frag = new ProcFrag(myframe.procEntryExit1(bodyStm), myframe);
    frag.next = frags;
    frags = frag;
  }
  //retrieving generated intermediate code fragments
  public Frag getResult() {
    return frags;
  }

  private static Tree.Exp CONST(int value) {
    return new Tree.CONST(value);
  }
  private static Tree.Exp NAME(Label label) {
    return new Tree.NAME(label);
  }
  private static Tree.Exp TEMP(Temp temp) {
    return new Tree.TEMP(temp);
  }
  private static Tree.Exp BINOP(int binop, Tree.Exp left, Tree.Exp right) {
    return new Tree.BINOP(binop, left, right);
  }
  private static Tree.Exp MEM(Tree.Exp exp) {
    return new Tree.MEM(exp);
  }
  private static Tree.Exp CALL(Tree.Exp func, Tree.ExpList args) {
    return new Tree.CALL(func, args);
  }
  private static Tree.Exp ESEQ(Tree.Stm stm, Tree.Exp exp) {
    if (stm == null)
      return exp;
    return new Tree.ESEQ(stm, exp);
  }

  private static Tree.Stm MOVE(Tree.Exp dst, Tree.Exp src) {
    return new Tree.MOVE(dst, src);
  }
  private static Tree.Stm UEXP(Tree.Exp exp) {
    return new Tree.UEXP(exp);
  }
  private static Tree.Stm JUMP(Label target) {
    return new Tree.JUMP(target);
  }
  private static Tree.Stm CJUMP(int relop, Tree.Exp l, Tree.Exp r, Label t, Label f) {
    return new Tree.CJUMP(relop, l, r, t, f);
  }
  private static Tree.Stm SEQ(Tree.Stm left, Tree.Stm right) {
    if (left == null)
      return right;
    if (right == null)
      return left;
    return new Tree.SEQ(left, right);
  }
  private static Tree.Stm LABEL(Label label) {
    return new Tree.LABEL(label);
  }

  private static Tree.ExpList ExpList(Tree.Exp head, Tree.ExpList tail) {
    return new Tree.ExpList(head, tail);
  }
  private static Tree.ExpList ExpList(Tree.Exp head) {
    return ExpList(head, null);
  }
  private static Tree.ExpList ExpList(ExpList exp) {
    if (exp == null)
      return null;
    return ExpList(exp.head.unEx(), ExpList(exp.tail));
  }

  public Exp Error() {
    return new Ex(CONST(0));
  }



  public Exp SimpleVar(Access access, Level level) {
    Tree.Exp framePointer = TEMP(frame.FP());
    Level currentLevel = level;
    
    while (currentLevel != access.home) {
      framePointer = currentLevel.parent.frame.formals.head.exp(framePointer);
      currentLevel = currentLevel.parent;
    }
  
    Tree.Exp location = access.acc.exp(framePointer);
    return new Ex(location);
  }


  public Exp FieldVar(Exp record, int index) {
        //NOT COMPLETE, havent been able to test it
    int offset = index * frame.wordSize();
    Temp pointer = new Temp();
    Tree.Stm pointerStm = MOVE(TEMP(pointer), record.unEx());
    
    Tree.Exp pointerExp = MEM(BINOP(Tree.BINOP.PLUS, TEMP(pointer), CONST(offset)));
    
    return new Ex(ESEQ(pointerStm, pointerExp));
  }

  public Exp SubscriptVar(Exp array, Exp index) {
    Tree.Exp offset = BINOP(Tree.BINOP.MUL, index.unEx(), CONST(frame.wordSize()));
		Temp pointerReg = new Temp();
		Tree.Stm pointerStm = MOVE(TEMP(pointerReg), array.unEx());
		Tree.Exp pointerExp = MEM(BINOP(Tree.BINOP.PLUS, TEMP(pointerReg), offset));
		return new Ex(ESEQ(pointerStm, pointerExp));
  }

  public Exp NilExp() {
    return new Ex(CONST(0));
  }

  public Exp IntExp(int value) {
    return new Ex(CONST(value));
  }

  private java.util.Hashtable strings = new java.util.Hashtable();
  public Exp StringExp(String lit) {
    String u = lit.intern();
    Label lab = (Label)strings.get(u);
    if (lab == null) {
      lab = new Label();
      strings.put(u, lab);
      DataFrag frag = new DataFrag(frame.string(lab, u));
      frag.next = frags;
      frags = frag;
    }
    return new Ex(NAME(lab));
  }

  private Tree.Exp CallExp(Symbol f, ExpList args, Level from) {
    return frame.externalCall(f.toString(), ExpList(args));
  }
  private Tree.Exp CallExp(Level f, ExpList args, Level from) {
    Tree.Exp fp = TEMP(from.frame.FP());
    if (f.parent != from) {
      for (Level l = from; l != f.parent; l = l.parent) {
        fp = l.frame.formals.head.exp(fp);
      }
    }
    return CALL(NAME(f.frame.name), ExpList(fp, ExpList(args)));
    }

  public Exp FunExp(Symbol f, ExpList args, Level from) {
    return new Ex(CallExp(f, args, from));
  }
  public Exp FunExp(Level f, ExpList args, Level from) {
    return new Ex(CallExp(f, args, from));
  }
  public Exp ProcExp(Symbol f, ExpList args, Level from) {
    return new Nx(UEXP(CallExp(f, args, from)));
  }
  public Exp ProcExp(Level f, ExpList args, Level from) {
    return new Nx(UEXP(CallExp(f, args, from)));
  }




  public Exp OpExp(int op, Exp left, Exp right) {
    Tree.Exp leftExp = left.unEx();
      Tree.Exp rightExp = right.unEx();
      switch(op) {
          case Absyn.OpExp.PLUS:
          case Absyn.OpExp.MINUS:
          case Absyn.OpExp.MUL:
          case Absyn.OpExp.DIV:
              return new Ex(BINOP(op, left.unEx(), right.unEx()));
          case Absyn.OpExp.EQ:
              return new RelCx(CJUMP.EQ, left.unEx(), right.unEx());
          case Absyn.OpExp.NE:
              return new RelCx(CJUMP.NE, left.unEx(), right.unEx());
          case Absyn.OpExp.GE:
              return new RelCx(CJUMP.GE, left.unEx(), right.unEx());
          case Absyn.OpExp.GT:
              return new RelCx(CJUMP.GT, left.unEx(), right.unEx());
          case Absyn.OpExp.LE:
              return new RelCx(CJUMP.LE, left.unEx(), right.unEx());
          case Absyn.OpExp.LT:
              return new RelCx(CJUMP.LT, left.unEx(), right.unEx());
          default:
               System.err.println("unknown operator "+op);
               return Error();
      }
  }

  public Exp StrOpExp(int op, Exp left, Exp right) {
    return new Ex(BINOP(op, left.unEx(), right.unEx()));
  }

  public Exp RecordExp(ExpList init) {
    //NOT COMPLETE
    //i think we will need a helper method for this one
    Temp recordTemp = new Temp();
    Tree.Stm seq = null;
    int allocSize = 0;
    
    for (ExpList e = init; e != null; e = e.tail) {
      seq = (seq == null) ? e.head.unNx() : SEQ(seq, e.head.unNx());
      allocSize += frame.wordSize();
    }
  
  Tree.Exp alloc = frame.externalCall("allocRecord", ExpList(CONST(allocSize)));
  Tree.Stm initRecord = MOVE(TEMP(recordTemp), alloc);
  
  return new Ex(ESEQ(SEQ(initRecord, seq), TEMP(recordTemp)));
  }

   public Exp SeqExp(ExpList e) {
    if (e == null)
      return new Nx(null);
    Tree.Stm stm = null;
    for (; e.tail != null; e = e.tail)
      stm = SEQ(stm, e.head.unNx());
    Tree.Exp result = e.head.unEx();
    if (result == null)
      return new Nx(SEQ(stm, e.head.unNx()));
    return new Ex(ESEQ(stm, result));
  }

  public Exp AssignExp(Exp lhs, Exp rhs) {
    return new Nx(MOVE(lhs.unEx(), rhs.unEx()));
  }

  public Exp IfExp(Exp cc, Exp aa, Exp bb) {
    return new IfThenElseExp(cc, aa, bb);
  }

  public Exp WhileExp(Exp test, Exp body, Label done) {
    //double check this
    Label testLabel = new Label();
    Label bodyLabel = new Label();
    Tree.Stm testStm = SEQ(LABEL(testLabel), test.unCx(bodyLabel, done));
    Tree.Stm bodyStm = SEQ(LABEL(bodyLabel), body.unNx());
    Tree.Stm left = SEQ(testStm, SEQ(bodyStm, JUMP(testLabel)));
    return new Nx(SEQ(left, LABEL(done)));
}

  public Exp ForExp(Access i, Exp lo, Exp hi, Exp body, Label done) {
    Temp home = i.home.frame.FP();
    Exp id = new Ex(i.acc.exp(TEMP(home)));
    return ForExp(id, lo, hi, body, done);
}

public Exp ForExp(Exp id, Exp lo, Exp hi, Exp body, Label done) {
    Temp iTemp = new Temp();
    Temp limitTemp = new Temp();
    Tree.Stm init_i = new Tree.MOVE(new Tree.TEMP(iTemp), lo.unEx());
    Tree.Stm init_limit = new Tree.MOVE(new Tree.TEMP(limitTemp), hi.unEx());
    
    Label testLabel = new Label();
    Tree.CJUMP condition = new Tree.CJUMP(Tree.CJUMP.LE, new Tree.TEMP(iTemp), new Tree.TEMP(limitTemp), testLabel, done);
    
    Tree.Stm update_i = new Tree.MOVE(new Tree.TEMP(iTemp), new Tree.BINOP(Tree.BINOP.PLUS, new Tree.TEMP(iTemp), new Tree.CONST(1)));
    
    // Combine body, update_i, and the condition for the while loop
    Tree.Stm whileBody = new Tree.SEQ(new Tree.LABEL(testLabel), new Tree.SEQ(body.unNx(), new Tree.SEQ(update_i, new Tree.SEQ(condition, new Tree.LABEL(done)))));
    
    // Set up the initial loop structure
    Tree.Stm whileLoop = new Tree.SEQ(init_i, new Tree.SEQ(init_limit, whileBody));
    
    return new Nx(whileLoop); // Wrap the whileLoop within an Nx object
}




  public Exp BreakExp(Label done) {
    return new Nx(JUMP(done));
    //should be sufficient
  }
  

  public Exp LetExp(ExpList lets, Exp body) {
    //i believe this is good
      if (lets == null) {
        return body;
    } else {
        Tree.Stm seq = null;
        for (ExpList e = lets; e != null; e = e.tail) {
            seq = (seq == null) ? e.head.unNx() : SEQ(seq, e.head.unNx());
        }
        return new Nx(SEQ(seq, body.unNx()));
    }
  }

public Exp ArrayExp(Exp size, Exp init) {
    Temp t = new Temp();
    Tree.Exp memSize = size.unEx();
    Tree.ESEQ arrayEseq = new Tree.ESEQ(new Tree.MOVE(new Tree.TEMP(t), frame.externalCall("initArray", ExpList(memSize, ExpList(init.unEx())))), new Tree.TEMP(t));
    return new Ex(arrayEseq);
}


 public Exp VarDec(Access a, Exp init) {
    if (a == null || a.acc == null || init == null) {
        // Handle the null case or throw a more meaningful exception
        return Error();
    }
    return new Nx(MOVE(a.acc.exp(TEMP(a.home.frame.FP())), init.unEx()));
}


  public Exp TypeDec() {
        //NOT COMPLETE, or its supposed to be like this but probably not

    return new Nx(null);
  }

  public Exp FunctionDec() {
        //NOT COMPLETE?? or its supposed to be this idk

    return new Nx(null);
  }
}
