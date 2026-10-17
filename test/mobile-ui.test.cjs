const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');

test('login opens immediately even when initial session lookup is still pending; slow sends show feedback',async()=>{
 const elements={},timers=[];let opened='';
 const element=id=>elements[id]??=( {value:'test@example.com',disabled:false,textContent:'',reportValidity:()=>true} );
 const page={window:{ShiguangCloud:{ready:new Promise(()=>{}),user:null,requestCode:()=>new Promise(()=>{})},addEventListener(){}},document:{getElementById:element},openModal:(title)=>opened=title,esc:x=>x,setTimeout:fn=>{timers.push(fn);return timers.length;},clearTimeout(){},Promise};
 vm.runInNewContext(fs.readFileSync('dist/cloud-ui.js','utf8'),page);
 element('cloud-account').onclick();assert.match(opened,/登录/);
 const pending=element('cloud-send').onclick({target:element('cloud-send')});
 assert.equal(element('cloud-feedback').textContent,'正在发送登录邮件…');assert.equal(element('cloud-send').disabled,true);
 timers[0]();await pending;assert.match(element('cloud-feedback').textContent,/连接超时/);assert.equal(element('cloud-send').disabled,false);
});

test('brand resolves to a real packaged page; removed floating collector is not wired up',()=>{
 const html=fs.readFileSync('dist/index.html','utf8'),href=html.match(/class="brand" href="([^"]+)"/)[1];
 assert.equal(new URL(href,'https://appassets.androidplatform.net/assets/index.html').pathname,'/assets/index.html');
 assert.ok(!html.includes('id="quick-add"'));assert.ok(!fs.readFileSync('dist/app.js','utf8').includes("$('#quick-add').onclick"));
});

test('native capture setup sends only the action explicitly tapped; website hides Android setup',()=>{
 const source=fs.readFileSync('dist/app.js','utf8').split('\n').find(line=>line.startsWith('function settings()'));
 let html='',sent=[];const controls={};
 const actions=['enableFloating','disableFloating','addCaptureTile','pinCapture','captureClipboard'];
 const buttons=actions.map(action=>({dataset:{nativeAction:action}}));
 const context={items:[],storageFailed:false,window:{ShiguangNative:{postMessage:raw=>sent.push(JSON.parse(raw))}},document:{documentElement:{dataset:{appVersion:'1.5.4'}},querySelectorAll:()=>buttons},esc:x=>x,openModal:(title,body)=>html=body,$:id=>controls[id]??={},backup(){},importDialog(){}};
 vm.runInNewContext(source+';settings()',context);
 assert.match(html,/Android 1.5.4/);assert.match(html,/开启悬浮收藏球/);assert.match(html,/关闭悬浮收藏球/);assert.equal(sent.length,0);
 buttons.forEach(button=>button.onclick());
 assert.deepEqual(sent,actions.map(action=>({action})));
 context.window={};vm.runInNewContext('settings()',context);assert.ok(!html.includes('data-native-action'));
});

test('brand returns to library without navigating to an Android asset directory',()=>{
 const source=fs.readFileSync('dist/app.js','utf8').split('\n').find(line=>line.startsWith("$('.brand').onclick="));
 const brand={};let prevented=false,reset=false,rendered=false,scrolled=false;
 vm.runInNewContext(source,{$:()=>brand,resetFilters:()=>reset=true,render:()=>rendered=true,window:{scrollTo:()=>scrolled=true}});
 brand.onclick({preventDefault:()=>prevented=true});assert.ok(prevented&&reset&&rendered&&scrolled);
});
