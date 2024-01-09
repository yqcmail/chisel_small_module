import chisel3._ 
import chisel3.util._
import chisel3.experimental.{prefix, noPrefix} 
import chisel3.stage.ChiselStage
import chisel3.experimental.ChiselEnum
import chisel3.experimental._

//class Ch_ccrw() extends RawModule with RequireAsyncReset{
//class Ary_ccrw(ccrw_len:Int ) extends RawModule {
class Ary_ccrw(mk_defau:Seq[(String,String)] ) extends RawModule {

val ccrw_len = mk_defau.length

val pclk_i   = IO(Input(Clock()))
val prstn_i  = IO(Input(AsyncReset()))
val wr_sel_i = IO(Input(UInt(ccrw_len.W)))
val data_i   = IO(Input(UInt(32.W)))   
val data_o   = IO(Output(Vec(ccrw_len,UInt(32.W))))

override val desiredName = s"ary_ccrw"

//val rst = (!rstn_i.asBool).asAsyncReset 

val prst = (!prstn_i.asBool).asAsyncReset

//val pclk_area = withClockAndReset(pclk_i,prst){   //clk area, regs must write in this
//   } // clk area, regs must in this area
//

// val m_cr_ary = VecInit(Seq.fill(ccrw_len)(Module(new Ccrw( 0x0ff,0x00 )).io))

//val m_cr_ary = VecInit(Seq(
 // //Module(new Ccrw( "32'hffffffff","32'h00" )).io, //00
 // Module(new Ccrw( "32'h"+"hffffffff".U.litValue().toString(16),"32'h00" )).io, //00
 // Module(new Ccrw( "32'hfe","32'h00" )).io, //01
 // Module(new Ccrw( "32'hfd","32'h00" )).io, //02
 // Module(new Ccrw( "32'hfc","32'h00" )).io, //03
 // Module(new Ccrw( "32'hfb","32'h00" )).io, //04
 // Module(new Ccrw( "32'hfa","32'h00" )).io, //05
 // Module(new Ccrw( "32'hf9","32'h00" )).io, //06
 // Module(new Ccrw( "32'hf8","32'h00" )).io, //07
 // Module(new Ccrw( "32'hf7","32'h00" )).io, //08
 // Module(new Ccrw( "32'hf6","32'h00" )).io, //09
//    ))
   
val m_cr_ary = VecInit(
 for ( (mk_vl,def_vl) <- mk_defau  ) yield { Module(new Ccrw( mk_vl,def_vl )).io }
    
    )

 for ( idx <- 0 until m_cr_ary.length ) {
      
      m_cr_ary(idx).rstn_i := prstn_i  
      m_cr_ary(idx).sclk_i := pclk_i
      m_cr_ary(idx).wr_i   := wr_sel_i(idx) 
      m_cr_ary(idx).dat_i  := data_i 
      data_o(idx) :=  m_cr_ary(idx).dat_o  
  }




// val m_cr_00 = Module(new Ccrw( 0x0ff,0x00 ))
// val m_cr_01 = Module(new Ccrw( 0x0ff,0x00 ))
// val m_cr_02 = Module(new Ccrw( 0x0ff,0x00 ))
// val m_cr_03 = Module(new Ccrw( 0x0ff,0x00 ))
// val m_cr_04 = Module(new Ccrw( 0x0ff,0x00 ))
// val m_cr_05 = Module(new Ccrw( 0x0ff,0x00 ))
// val m_cr_06 = Module(new Ccrw( 0x0ff,0x00 ))
// val m_cr_07 = Module(new Ccrw( 0x0ff,0x00 ))
// val m_cr_08 = Module(new Ccrw( 0x0ff,0x00 ))
// val m_cr_09 = Module(new Ccrw( 0x0ff,0x00 ))
//
// val m_cr_ary = Seq(
//  m_cr_00, m_cr_01, m_cr_02, m_cr_03, m_cr_04, 
//  m_cr_05, m_cr_06, m_cr_07, m_cr_08, m_cr_09 )

// for ( idx <- 0 until m_cr_ary.length ) {
//      m_cr_ary(idx).io.rstn_i := prstn_i  
//      m_cr_ary(idx).io.sclk_i := pclk_i
//      m_cr_ary(idx).io.wr_i   := 1.U 
//      m_cr_ary(idx).io.dat_i  := data_i(idx) 
//      data_o(idx) :=  m_cr_ary(idx).io.dat_o  
//  }





} //end_class


//with HasExtModulePath
class Ccrw(bs: String, dv : String) extends BlackBox( 
     Map("BS" ->  RawParam(bs ), 
         "DV" ->  RawParam(dv ) 
       )


  ) with HasBlackBoxResource {
    val io = IO( new Bundle{
           val  rstn_i = Input(AsyncReset())
           val  sclk_i = Input(Clock())
           val  wr_i   = Input(UInt(1.W))
           val   dat_i = Input(UInt(32.W))
           val   dat_o = Output(UInt(32.W))
    })



   // hw/chisel/resources/ccrw.v
  addResource("/ccrw.v")
 
//  addPath("/data/chip03/hosts/home1/qyuan/prj/chisel_prj/Pid_prj/hw/chisel/resources")



} //end_class








object Main extends App {
  (new chisel3.stage.ChiselStage)
    //.emitVerilog(new Ary_ccrw(10), 
    .emitVerilog(new Ary_ccrw(Seq(
      ("32'hfff0","32'h01"),
      ("32'hfff1","32'h01"),
      ("32'hfff8","32'h01"),
      ("32'hfff0","32'h01"),
      ("32'hfff1","32'h01"),
      ("32'hfff8","32'h01"),
    )), 
     Array(
        "--target-dir", "../../builds",
        "-e", "verilog",
        "--emission-options=disableMemRandomization,disableRegisterRandomization"
        
      )
    )

}
