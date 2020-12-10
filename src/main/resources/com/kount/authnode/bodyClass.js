var body = document.body;
function t1(){
        var call= localStorage.getItem("kountCall");
        if(!call){
            window["client"] = new ka.ClientSDK();
            client.autoLoadEvents()
            localStorage.setItem("kountCall", "1");
        }
        
    }
document.body.onload = new  function () {     
        var div = document.createElement('div');
		// set style
		div.setAttribute('class', 'kaxsdc'); 
		div.setAttribute("data-event","load");
		// and make sure myclass has some styles in css
		document.body.appendChild(div);
		window. onbeforeunload = function (e) { localStorage. clear(); };
		document.getElementsByTagName("BODY")[0].onmouseover = function(){t1();};
    }
 

