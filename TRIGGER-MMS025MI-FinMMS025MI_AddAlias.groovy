/**
 * README
 * This extension is being triggered by MMS025MI/AddAlias/POST
 *
 * Name: FinMMS025MI_AddAlias
 * Description: XTend made to insert all the enreg in MITPOP for the POPN in entry in the CONO 0,
 *              and delete them in the CONO in entry
 *              for after the API can create without unicity problem the POPN in entry
 * Date       Changed By                     Description
 * 20220601   Ludovic TRAVERS                Create CUS_xt_MMS025MI_AddAlias_POST Extension
 * 20221018   Ludovic TRAVERS                Fix post review issues
 * 20231010   François LEPREVOST             Fix post review issues
 */
 
public class FinMMS025MI_AddAlias extends ExtendM3Trigger {
  private final ProgramAPI program
  private final DatabaseAPI database
  private final MICallerAPI miCaller
  
  String POPN
  TableRecord recordFound
  int company = 0

  
  public FinMMS025MI_AddAlias(ProgramAPI program, DatabaseAPI database, MICallerAPI miCaller) {
    this.program = program
    this.database = database
    this.miCaller = miCaller
  }
  
  public void main() {
    recordFound = program.getTableRecord("MITPOP")
    int alwt = Integer.parseInt(recordFound["MPALWT"].toString())
    if (alwt == 2) {
      company = (Integer) recordFound["MPCONO"]
      
      POPN = recordFound["MPPOPN"]
      
      DBAction query = database.table("MITPOP").index("20").selectAllFields().build();
                  
      DBContainer container = query.getContainer()
      container.set("MPCONO", 200)
      container.set("MPALWT", 2)
      container.set("MPALWQ", recordFound["MPALWQ"])
      container.set("MPPOPN", POPN)
      
      Closure<?> releasedItemProcessor = {
        DBContainer data -> 
          addActualsDatsInUserCono(data)

          Closure<?> deleterCallback = { LockedResult lockedResult ->
            lockedResult.delete()
          }
          
          if (query.read(data)) {
            query.readLock(data, deleterCallback)
          } 
      }
      
      query.readAll(container, 4, releasedItemProcessor)
    }
  }
  
  private void addActualsDatsInUserCono(DBContainer data) {
    int alwt = 2
    String alwq = recordFound["MPALWQ"]
    String itno = data.get("MPITNO")
    String e0pa = recordFound["MPE0PA"]
    String sea1 = recordFound["MPSEA1"]
    int vfdt = (Integer) recordFound["MPVFDT"]

    int lvdt = data.get("MPLVDT")
    double cnqt = data.get("MPCNQT")
    String alun = data.get("MPALUN")
    String orco = data.get("MPORCO")
    int seqn = data.get("MPSEQN")
    String remk = data.get("MPREMK")
    String atpe = data.get("MPATPE")
    long atnr = data.get("MPATNR")
    long cfin = data.get("MPCFIN")
    String prna = data.get("MPPRNA")
    long txid = data.get("MPTXID")
    int rgdt = data.get("MPRGDT")
    int rgtm = data.get("MPRGTM")
    int lmdt = data.get("MPLMDT")
    int chno = data.get("MPCHNO")
    String chid = data.get("MPCHID")
  
    DBAction query = database.table("MITPOP").index("00").selectAllFields().build()
    DBContainer container = query.getContainer()
    container.set("MPCONO", company)
    container.set("MPALWT", alwt)
    container.set("MPALWQ", alwq)
    container.set("MPITNO", itno)
    container.set("MPPOPN", POPN)
    container.set("MPE0PA", e0pa)
    container.set("MPSEA1", sea1)
    container.set("MPVFDT", vfdt)

    if (!query.read(container)) {
      container.set("MPLVDT", lvdt)
      container.set("MPCNQT", cnqt)
      container.set("MPALUN", alun)
      container.set("MPORCO", orco)
      container.set("MPSEQN", seqn)
      container.set("MPREMK", remk)
      container.set("MPATPE", atpe)
      container.set("MPATNR", atnr)
      container.set("MPCFIN", cfin)
      container.set("MPPRNA", prna)
      container.set("MPTXID", txid)
      container.set("MPRGDT", rgdt)
      container.set("MPRGTM", rgtm)
      container.set("MPLMDT", lmdt)
      container.set("MPCHNO", chno)
      container.set("MPCHID", chid)
      
      query.insert(container)
    }
  }
}  