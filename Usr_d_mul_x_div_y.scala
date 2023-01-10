import chisel3._ 
import chisel3.util._
import chisel3.experimental.{prefix, noPrefix} 
import chisel3.stage.ChiselStage
import chisel3.experimental.ChiselEnum

//data_i*x_i/y_i
class Usr_d_mul_x_div_y(Dw:Int) extends Module with RequireAsyncReset{
  // override val desiredName = s"Usr_d_mul_x_div_y_$Dw" 
  val start_i = IO(Input (UInt(1.W)))
  val data_i = IO(Input (UInt(Dw.W)))
  val x_i = IO(Input (UInt(Dw.W)))
  val y_i = IO(Input (UInt(Dw.W)))
  val data_o = IO(Output (UInt(Dw.W)))
  val rem_o = IO(Output (UInt(Dw.W)))
  val don_o = IO(Output (UInt(1.W)))

   val s_idl :: s_mul_s  ::  s_div_s :: s_end :: s_done :: Nil = Enum(5)

   val rg_state = RegInit(s_idl)

   val cnt_s  = RegInit(0.U(log2Up(Dw+2).W))

   val y_not0 = Mux(y_i === 0.U,1.U,y_i)

 //state jump
   switch(rg_state){
    is( s_idl){
        cnt_s := 0.U
        when(start_i === 1.U){
         rg_state := s_mul_s
        }
    } 
    is(s_mul_s){
        cnt_s := cnt_s + 1.U
        when( cnt_s >= (Dw-1).asUInt){
         rg_state := s_div_s
         cnt_s := 0.U
        }
    } 
    is(s_div_s){
        cnt_s := cnt_s + 1.U
        when(cnt_s >= (Dw).asUInt){
          rg_state := s_end
          cnt_s := 0.U
        }
    }  
    is(s_end){
       rg_state := s_done
    } 
    is(s_done){
       rg_state := s_idl
    }
   } 
   
  //state logic
  // mul:  data_i*x_i  =  for( mul_acc = mul_acc  +  {if(x_abs[i])? data_i << i : 0   }  ),  if(x_i>0)? mul_acc : - mul_acc
  // div:  ext_div_a = s_mul_s_acc, ext_div_b = {y_i,n},    init( buf_ext_a = ext_div_a,)   run(buf_ext_a = if_lt_sub_shift1) 
  // if_lt_sub_shift1:   if(buf_ext_a >= ext_b) ?  (buf_ext_a - ext_b -1)<<1 :  buf_ext_a<<1 
  // div_q :   div_q(cnt-1) = if(buf_ext_a >= ext_b) ? 1 : 0 
  // div_r :  div_r = buf_ext_a(h_bit)  

  //val mul_acc = RegInit(0.U((Dw*2).W))
  val mul_acc = WireDefault(0.U) 


  def fun_mul_bit():UInt = {
    val mul_bit_shift = Mux(x_i(cnt_s) === 1.U , data_i << cnt_s, 0.U) 
     return  mul_bit_shift
  }

 //val ext_div_a = RegInit(0.U((Dw*2).W))
 val ext_div_a = WireDefault(0.U) 


 val ext_div_b = Cat(y_not0,0.U((Dw).W)) 
 val rg_div_q = RegInit(0.U(Dw.W))
 val rg_div_r = RegInit(0.U(Dw.W))
 val rg_don = RegInit(0.U(1.W))

  val com_add_o = WireDefault(0.U)
  val com_add_a = WireDefault(0.U)
  val com_add_b = WireDefault(0.U)
      com_add_o := com_add_a + com_add_b 

 def fun_lt_sub_shift(data_i:UInt ):UInt ={
     val lt_ab    = (data_i >= ext_div_b ) 
     val lt_div_b = Mux(lt_ab, (( data_i - ext_div_b) + 1.U)   , data_i)
 //    val lt_div_b = Mux(lt_ab, ( com_add_o  + 1.U)   , data_i)
     return   lt_div_b<<1

   }


  
  val rg_buf_d = RegInit(0.U((Dw*2+1).W))
  when(rg_state === s_idl){ 
      rg_buf_d := mul_acc 
  } .elsewhen(rg_state === s_mul_s){
      rg_buf_d := mul_acc 
  } .elsewhen(rg_state === s_div_s){
      rg_buf_d := ext_div_a 
  } 

  when(rg_state === s_idl){
        mul_acc := 0.U
        ext_div_a := 0.U
        rg_don := 0.U
        com_add_a := 0.U
        com_add_b := 0.U
        rg_div_r :=  0.U             
    } .elsewhen(rg_state === s_mul_s){
           //mul_acc := mul_acc + fun_mul_bit()
           mul_acc := rg_buf_d + fun_mul_bit()
           com_add_a := rg_buf_d
           com_add_b := fun_mul_bit()   
           //mul_acc := com_add_o 
          
        when( cnt_s >= (Dw-1).asUInt){
          //ext_div_a := mul_acc 
          ext_div_a := rg_buf_d 

        }


    } .elsewhen(rg_state === s_div_s){
         //ext_div_a := fun_lt_sub_shift(ext_div_a) 
         ext_div_a := fun_lt_sub_shift(rg_buf_d) 
         com_add_a := rg_buf_d 
         com_add_b := -ext_div_b
       
        when(cnt_s >= (Dw).asUInt){
        }

    } .elsewhen(rg_state === s_end){ 
        rg_div_q := rg_buf_d(Dw,1)
        rg_div_r := rg_buf_d(Dw*2,Dw+1)             
    } .elsewhen(rg_state === s_done){ 
        rg_don := 1.U
    
    }


  //////////////////////  
   data_o := rg_div_q 
   don_o := rg_don 
   rem_o := rg_div_r  
}

object Main extends App {
  (new chisel3.stage.ChiselStage)
    .emitVerilog(new Usr_d_mul_x_div_y(Dw=8), 
     Array(
        "--target-dir", "../../builds",
        "-e", "verilog",
        "--emission-options=disableMemRandomization,disableRegisterRandomization"
      )
    )

}

