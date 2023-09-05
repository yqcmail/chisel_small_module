function! Chi_sta_seq(...)
if(len(a:000)==0) ||(a:000[0]== "h" || a:000[0]== "-h" || a:000[0]== "help" )
   echo "Chi_sta_seq(sta_name0,sta_name1,....)"
else
  
"   let reg_name =(len(a:000)>0)?(a:000[0]):"xxxx"
  let len_sta = len(a:000)
  let bit_w =  float2nr(ceil(log(len_sta)/log(2))) 
  let ary_sta = a:000
  
  let g_cd_clk_name ="clk"  
  let g_cd_rstn_name ="rstn" 

" let str_ary=[]
" call add(str_ary, \"      ____     ____         \")
  let str_ary = []  
  let t_str=""
  let t_str=t_str." val "

  for idx in range(0,len_sta-1) 
     let t_str = t_str."s".idx."_".ary_sta[idx]." :: "
  endfor 
  let t_str = t_str." Nil = Enum(".len_sta.")"
  call add(str_ary,t_str)

"""" 
  call add(str_ary, " val rg_sta = RegInit("."s".0."_".ary_sta[0].")")
  call add(str_ary, "")



  call add(str_ary, "   ////////////////////////")

  for idx in range(0,len_sta-1)
    if(idx < len_sta-1)
      call add(str_ary, "   //"."s".idx."_".ary_sta[idx]." ---> "."s".(idx+1)."_".ary_sta[idx+1])
    else
      call add(str_ary, "   //"."s".idx."_".ary_sta[idx]." ---> "."s".0."_".ary_sta[0])
    endif
  endfor 


  call add(str_ary, "   ")
  call add(str_ary, "   //state jump")
  call add(str_ary, " switch(rg_sta){")
 
  for idx in range(0,len_sta-1) 
  call add(str_ary, "   is("."s".idx."_".ary_sta[idx]."){")
    if(idx == len_sta-1)
     call add(str_ary,"     rg_sta := "."s".0."_".ary_sta[0])
    else
     call add(str_ary,"     rg_sta := "."s".(idx+1)."_".ary_sta[idx+1])
    endif
  call add(str_ary, "   }")
  endfor 
  call add(str_ary, " }") 

  
  call add(str_ary, "   ////////////////////////")
  call add(str_ary, "   //state logic")
  for idx in range(0,len_sta-1) 
    if(idx == 0)
      call add(str_ary, " when(rg_sta === "."s".idx."_".ary_sta[idx]."){")
    else
      call add(str_ary, " }.elsewhen(rg_sta === "."s".idx."_".ary_sta[idx]."){")
    endif
    call add(str_ary, "   ")
  endfor                         
 
  call add(str_ary, " }")

 call add(str_ary, "   ////////////////////////")
 call add(str_ary, "   //mux sel")
  for idx in range(0,len_sta-1) 
    if(idx == 0)
      call add(str_ary, " when(rg_sta === "."s".idx."_".ary_sta[idx]."){")
    else
      call add(str_ary, " }.elsewhen(rg_sta === "."s".idx."_".ary_sta[idx]."){")
    endif
    call add(str_ary, "   ")
  endfor                         
 
  call add(str_ary, " }")



  call append(line("."),str_ary)             

endif     

endfunction 

