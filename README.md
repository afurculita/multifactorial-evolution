Evolutionary Multitasking Implementation
----------------------------------------

Evolutionary computation (EC) has gained increasing popularity in dealing with permutation-based combinatorial 
optimization problems (PCOPs). Traditionally, EC focuses on solving a single optimization task at a time. 
However, in complex multi-echelon supply chain networks (SCNs), there usually exist various kinds of PCOPs at 
the same time, e.g., travel salesman problem (TSP), job-shop scheduling problem (JSP), etc. So, it is desirable 
to solve several PCOPs at once with both effectiveness and efficiency. Very recently, a new paradigm in EC, namely,
multifactorial optimization (MFO) has been introduced to explore the potential of evolutionary multitasking, 
which can serve the purpose of simultaneously optimizing multiple PCOPs in SCNs.

This implementation is based on the papers ["Multifactorial Evolution: Towards Evolutionary Multitasking", by 
Abhishek Gupta, Yew-Soon Ong, and Liang Feng](https://paperpile.com/app/p/cea64444-3e2a-06ea-a463-df2e14feb76e) and
["Evolutionary Multitasking in Permutation-Based Combinatorial Optimization Problems: Realization  with TSP, QAP, 
LOP, and JSP" by Yuan Yuan, Yew-Soon Ong, Abhishek Gupta, Puay Siew Tan and Hua Xu](https://paperpile.com/app/p/35c56229-5810-0e69-9976-cae8a1c1379d).