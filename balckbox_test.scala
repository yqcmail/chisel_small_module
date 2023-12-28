import chisel3._ 
import chisel3.util._
import chisel3.experimental.{prefix, noPrefix} 
import chisel3.stage.ChiselStage
import chisel3.experimental.ChiselEnum

//class Ch_ccrw() extends RawModule with RequireAsyncReset{
class Ary_ccrw extends RawModule {
val clk_i  = IO(Input(Clock()))
val rstn_i  = IO(Input(AsyncReset()))
val fs_en_i    = IO(Input(UInt(1.W)))

val pclk_i   = IO(Input(Clock()))
val prstn_i  = IO(Input(AsyncReset()))

val rst = (!rstn_i.asBool).asAsyncReset 

val prst = (!prstn_i.asBool).asAsyncReset

 val pclk_area = withClockAndReset(pclk_i,prst){   //clk area, regs must write in this



   } // clk area, regs must in this area

 val m_cr00 = Module(new Ccrw(
      //Map("BS" -> "hfffff000".U,
      //    "DV" -> "h00000000".U
      //  )
   ))

      m_cr00.io.rstn_i := prstn_i  
      m_cr00.io.sclk_i := pclk_i
      m_cr00.io.wr_i   := 1.U 
      m_cr00.io.dat_i  := "h55".U
      //     m_cr00.dat_o  


} //end_class


//with HasExtModulePath
class Ccrw() extends BlackBox(
      Map("BS" -> 0xfffff000,
          "DV" -> 0x00
        )
  ) with HasBlackBoxResource {
    val io = IO( new Bundle{
           val  rstn_i = Input(AsyncReset())
           val  sclk_i = Input(Clock())
           val  wr_i   = Input(UInt(1.W))
           val   dat_i = Input(UInt(1.W))
           val   dat_o = Output(UInt(1.W))
    })



   // hw/chisel/resources/ccrw.v
  addResource("/ccrw.v")
 
//  addPath("/data/chip03/hosts/home1/qyuan/prj/chisel_prj/Pid_prj/hw/chisel/resources")



} //end_class








object Main extends App {
  (new chisel3.stage.ChiselStage)
    .emitVerilog(new Ary_ccrw(), 
     Array(
        "--target-dir", "../../builds",
        "-e", "verilog",
        "--emission-options=disableMemRandomization,disableRegisterRandomization"
        
      )
    )

}
