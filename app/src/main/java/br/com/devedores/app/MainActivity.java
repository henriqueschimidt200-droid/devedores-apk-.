package br.com.devedores.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    DataStore ds; LinearLayout root, clientList; EditText search; int filterIndex=0;

    @Override public void onCreate(Bundle b){
        super.onCreate(b); ds=new DataStore(this); requestNotif(); build();
        if(!AlarmScheduler.canExact(this)) new AlertDialog.Builder(this)
            .setTitle("Ativar lembretes")
            .setMessage("Para o celular avisar na hora certa mesmo com o app fechado, ative 'Alarmes e lembretes'.")
            .setPositiveButton("Ativar",(d,w)->AlarmScheduler.openExactSettings(this))
            .setNegativeButton("Depois",null).show();
    }
    void requestNotif(){ if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},77); }
    @Override protected void onResume(){super.onResume();if(ds!=null){ds.load();if(root!=null)render();AlarmScheduler.rescheduleAll(this);}}
    void build(){root=Ui.col(this);ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setVerticalScrollBarEnabled(false);sc.addView(root);setContentView(sc);Ui.applySystemBars(this,root);}
    String money(double x){return String.format(Locale.getDefault(),"R$ %.2f",x);}

    void render(){root.removeAllViews();header();mainCard();quickActions();importantToday();clientsSection();nextPayments();moreSection();}

    void header(){
        LinearLayout r=Ui.row(this); TextView brand=Ui.iconBadge(this,"D"); r.addView(brand,new LinearLayout.LayoutParams(Ui.dp(this,52),Ui.dp(this,52))); Ui.gap(this,r,10);
        LinearLayout t=Ui.col(this);t.setPadding(0,0,0,0);t.addView(Ui.title(this,"Devedores",26));t.addView(Ui.label(this,"Bem-vinda! Vamos deixar tudo simples."));r.addView(t,new LinearLayout.LayoutParams(0,Ui.dp(this,58),1));
        Button help=Ui.btnDark(this,"Ajuda");help.setOnClickListener(v->showHelp());r.addView(help,new LinearLayout.LayoutParams(Ui.dp(this,70),Ui.dp(this,52)));root.addView(r);Ui.gap(this,root,12);
    }

    void mainCard(){
        double balance=0,paid=0,total=0;int overdue=0;long now=System.currentTimeMillis();
        for(Models.Client c:ds.clients)for(Models.Loan l:c.loans){balance+=l.balance();paid+=l.paid();total+=l.total;for(int i=0;i<l.installments;i++)if(!l.installmentPaid(i)&&l.dueAt(i)<now)overdue++;}
        LinearLayout h=Ui.heroCard(this,overdue>0?Ui.RED:Ui.GOLD);h.addView(Ui.eyebrow(this,overdue>0?"TEM PAGAMENTOS ATRASADOS":"TUDO TRANQUILO"));h.addView(Ui.title(this,money(balance),32));h.addView(Ui.label(this,"Você tem para receber"));Ui.gap(this,h,10);
        LinearLayout r=Ui.row(this);r.addView(Ui.infoTile(this,"RECEBIDO",money(paid),Ui.GREEN),new LinearLayout.LayoutParams(0,Ui.dp(this,76),1));Ui.gap(this,r,6);r.addView(Ui.infoTile(this,"CONTRATADO",money(total),Ui.BLUE),new LinearLayout.LayoutParams(0,Ui.dp(this,76),1));h.addView(r);Ui.gap(this,h,10);
        Button b=Ui.btnDark(this,overdue>0?"Ver pagamentos atrasados":"Ver próximos pagamentos");b.setOnClickListener(v->startActivity(new Intent(this,CalendarActivity.class)));h.addView(b,new LinearLayout.LayoutParams(-1,Ui.dp(this,56)));root.addView(h);Ui.gap(this,root,14);
    }

    void quickActions(){
        root.addView(Ui.sectionTitle(this,"O que você quer fazer?"));Ui.gap(this,root,6);
        LinearLayout r1=Ui.row(this),r2=Ui.row(this);
        Button add=Ui.bigBtn(this,"👤  Novo cliente",Ui.GOLD);add.setOnClickListener(v->startActivity(new Intent(this,AddClientActivity.class)));
        Button pay=Ui.bigBtn(this,"💰  Receber pagamento",Ui.GREEN);pay.setOnClickListener(v->chooseClientForLoan());
        Button cli=Ui.bigBtn(this,"📒  Ver clientes",Ui.BLUE);cli.setOnClickListener(v->{ if(search!=null){search.requestFocus();} else {Toast.makeText(this,"Role até Clientes",Toast.LENGTH_SHORT).show();}});
        Button cal=Ui.bigBtn(this,"📅  Calendário",Ui.PURPLE);cal.setOnClickListener(v->startActivity(new Intent(this,CalendarActivity.class)));
        r1.addView(add,new LinearLayout.LayoutParams(0,Ui.dp(this,76),1));Ui.gap(this,r1,8);r1.addView(pay,new LinearLayout.LayoutParams(0,Ui.dp(this,76),1));
        r2.addView(cli,new LinearLayout.LayoutParams(0,Ui.dp(this,76),1));Ui.gap(this,r2,8);r2.addView(cal,new LinearLayout.LayoutParams(0,Ui.dp(this,76),1));
        root.addView(r1);Ui.gap(this,root,8);root.addView(r2);Ui.gap(this,root,16);
    }

    void importantToday(){
        long now=System.currentTimeMillis();int late=0;double lateValue=0;for(Models.Client c:ds.clients)for(Models.Loan l:c.loans)for(int i=0;i<l.installments;i++)if(!l.installmentPaid(i)&&l.dueAt(i)<now){late++;lateValue+=Math.max(0,l.installmentAmount-l.paidForInstallment(i));}
        LinearLayout card=Ui.softCard(this,late>0?Ui.RED:Ui.GREEN);card.addView(Ui.eyebrow(this,"HOJE"));card.addView(Ui.title(this,late>0?late+" pagamento(s) atrasado(s)":"Nenhum pagamento atrasado",20));card.addView(Ui.label(this,late>0?money(lateValue)+" em atraso. Toque abaixo para ver.":"Sua agenda está em dia."));Button b=Ui.btn(this,late>0?"Abrir atrasados":"Abrir calendário");b.setOnClickListener(v->startActivity(new Intent(this,CalendarActivity.class)));Ui.gap(this,card,6);card.addView(b,new LinearLayout.LayoutParams(-1,Ui.dp(this,54)));root.addView(card);Ui.gap(this,root,16);
    }

    void clientsSection(){
        root.addView(Ui.sectionHeader(this,"Meus clientes","+ Novo",v->startActivity(new Intent(this,AddClientActivity.class))));Ui.gap(this,root,4);
        search=Ui.searchField(this,"🔎  Digite o nome do cliente");search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){renderClientList();}public void afterTextChanged(Editable e){}});root.addView(search,new LinearLayout.LayoutParams(-1,Ui.dp(this,56)));Ui.gap(this,root,7);
        LinearLayout chips=Ui.row(this);String[] names={"Todos","Em aberto","Atrasados","Quitados"};for(int i=0;i<names.length;i++){final int idx=i;Button b=Ui.btnGhost(this,names[i]);b.setTextSize(13);b.setTextColor(idx==filterIndex?Ui.BG:Ui.MUTED);b.setBackground(UiChip(idx==filterIndex?Ui.GOLD:Ui.SURFACE_2));b.setOnClickListener(v->{filterIndex=idx;render();});chips.addView(b,new LinearLayout.LayoutParams(0,Ui.dp(this,42),1));if(i<3)Ui.gap(this,chips,4);}root.addView(chips);Ui.gap(this,root,8);
        clientList=Ui.col(this);clientList.setPadding(0,0,0,0);root.addView(clientList);renderClientList();Ui.gap(this,root,16);
    }
    android.graphics.drawable.GradientDrawable UiChip(int bg){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(bg);g.setCornerRadius(Ui.dp(this,16));g.setStroke(Ui.dp(this,1),Ui.BORDER);return g;}

    void renderClientList(){
        if(clientList==null)return;clientList.removeAllViews();int shown=0;
        for(Models.Client c:ds.clients){if(!matches(c))continue;shown++;double bal=0;int overdue=0;long next=Long.MAX_VALUE;for(Models.Loan l:c.loans){bal+=l.balance();for(int i=0;i<l.installments;i++){if(l.installmentPaid(i))continue;long d=l.dueAt(i);if(d<System.currentTimeMillis())overdue++;else next=Math.min(next,d);}}
            LinearLayout card=Ui.card(this);card.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16));LinearLayout top=Ui.row(this);View av=(c.profileImagePath!=null&&!c.profileImagePath.isEmpty())?Ui.profileImage(this,c.profileImagePath,c.name,58):Ui.avatar(this,c.name);top.addView(av,new LinearLayout.LayoutParams(Ui.dp(this,58),Ui.dp(this,58)));LinearLayout tx=Ui.col(this);tx.setPadding(Ui.dp(this,12),0,0,0);tx.addView(Ui.title(this,c.name,18));tx.addView(Ui.label(this,c.phone.isEmpty()?"Sem telefone":c.phone));top.addView(tx,new LinearLayout.LayoutParams(0,Ui.dp(this,62),1));top.addView(Ui.pill(this,overdue>0?"Atrasado":bal>0?"Em aberto":"Quitado",overdue>0?Ui.RED:bal>0?Ui.BLUE:Ui.GREEN,Ui.WHITE));card.addView(top);Ui.gap(this,card,8);card.addView(Ui.title(this,bal>0?money(bal):"Nada pendente",20));card.addView(Ui.label(this,next==Long.MAX_VALUE?"Sem próximo vencimento":("Próximo: "+new SimpleDateFormat("dd/MM • HH:mm",Locale.getDefault()).format(new Date(next)))));Ui.gap(this,card,9);LinearLayout actions=Ui.row(this);Button open=Ui.btn(this,"Abrir cliente");final String id=c.id;open.setOnClickListener(v->{Intent i=new Intent(this,ClientActivity.class);i.putExtra("id",id);startActivity(i);});Button pay=Ui.btnDark(this,"Receber");pay.setOnClickListener(v->{Intent i=new Intent(this,ClientActivity.class);i.putExtra("id",id);startActivity(i);});actions.addView(open,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1));Ui.gap(this,actions,6);actions.addView(pay,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1));card.addView(actions);clientList.addView(card);Ui.gap(this,clientList,10);
        }
        if(ds.clients.isEmpty()){LinearLayout e=Ui.heroCard(this,Ui.GOLD);e.addView(Ui.title(this,"Vamos cadastrar seu primeiro cliente",21));e.addView(Ui.label(this,"Depois disso você poderá colocar fotos, documentos, empréstimos e parcelas."));Ui.gap(this,e,8);Button b=Ui.bigBtn(this,"＋  Cadastrar cliente",Ui.GOLD);b.setOnClickListener(v->startActivity(new Intent(this,AddClientActivity.class)));e.addView(b);clientList.addView(e);}else if(shown==0){LinearLayout e=Ui.softCard(this,Ui.BLUE);e.addView(Ui.title(this,"Cliente não encontrado",16));e.addView(Ui.label(this,"Tente outro nome ou escolha 'Todos'."));clientList.addView(e);}
    }

    boolean matches(Models.Client c){String q=search==null?"":search.getText().toString().trim().toLowerCase(Locale.getDefault());boolean txt=q.isEmpty()||c.name.toLowerCase(Locale.getDefault()).contains(q)||c.cpf.toLowerCase(Locale.getDefault()).contains(q)||c.phone.toLowerCase(Locale.getDefault()).contains(q);double bal=0;boolean overdue=false;for(Models.Loan l:c.loans){bal+=l.balance();for(int i=0;i<l.installments;i++)if(!l.installmentPaid(i)&&l.dueAt(i)<System.currentTimeMillis())overdue=true;}return txt&&(filterIndex==0||(filterIndex==1&&bal>0.005)||(filterIndex==2&&overdue)||(filterIndex==3&&bal<=0.005));}

    void nextPayments(){
        root.addView(Ui.sectionTitle(this,"Próximos pagamentos"));Ui.gap(this,root,6);long now=System.currentTimeMillis();int shown=0;List<String> lines=new ArrayList<>();
        for(Models.Client c:ds.clients)for(Models.Loan l:c.loans)for(int i=0;i<l.installments;i++){if(l.installmentPaid(i)||l.dueAt(i)<now)continue;long d=l.dueAt(i);lines.add(new SimpleDateFormat("yyyyMMddHHmm",Locale.getDefault()).format(new Date(d))+"|"+c.name+"|"+money(Math.max(0,l.installmentAmount-l.paidForInstallment(i)))+"|"+new SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(new Date(d)));}
        Collections.sort(lines);for(String s:lines){String[] p=s.split("\\|");LinearLayout c=Ui.card(this);LinearLayout r=Ui.row(this);r.addView(Ui.pill(this,p[3],Ui.GOLD,Ui.WHITE),new LinearLayout.LayoutParams(Ui.dp(this,86),Ui.dp(this,36)));LinearLayout tx=Ui.col(this);tx.setPadding(Ui.dp(this,10),0,0,0);tx.addView(Ui.title(this,p[1],15));tx.addView(Ui.label(this,p[2]+" para receber"));r.addView(tx,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));c.addView(r);root.addView(c);Ui.gap(this,root,7);shown++;if(shown>=4)break;}if(shown==0)root.addView(Ui.softCard(this,Ui.GREEN));if(shown==0){LinearLayout x=(LinearLayout)root.getChildAt(root.getChildCount()-1);x.addView(Ui.title(this,"Nenhum pagamento próximo",15));x.addView(Ui.label(this,"Tudo tranquilo por enquanto."));}Ui.gap(this,root,16);
    }

    void moreSection(){
        root.addView(Ui.sectionTitle(this,"Mais opções"));Ui.gap(this,root,6);LinearLayout r=Ui.row(this);Button rel=Ui.btnDark(this,"Relatórios");rel.setOnClickListener(v->startActivity(new Intent(this,ReportsActivity.class)));Button ex=Ui.btnDark(this,"Backup");ex.setOnClickListener(v->exportBackup());Button im=Ui.btnDark(this,"Restaurar");im.setOnClickListener(v->importBackup());r.addView(rel,new LinearLayout.LayoutParams(0,Ui.dp(this,54),1));Ui.gap(this,r,5);r.addView(ex,new LinearLayout.LayoutParams(0,Ui.dp(this,54),1));Ui.gap(this,r,5);r.addView(im,new LinearLayout.LayoutParams(0,Ui.dp(this,54),1));root.addView(r);Ui.gap(this,root,12);LinearLayout f=Ui.softCard(this,Ui.BLUE);f.addView(Ui.title(this,"Salva automaticamente",15));f.addView(Ui.label(this,"Clientes, fotos, documentos, pagamentos e lembretes ficam guardados neste celular."));root.addView(f);
    }

    void chooseClientForLoan(){if(ds.clients.isEmpty()){startActivity(new Intent(this,AddClientActivity.class));return;}String[] n=new String[ds.clients.size()];for(int i=0;i<ds.clients.size();i++)n[i]=ds.clients.get(i).name;new AlertDialog.Builder(this).setTitle("Escolha o cliente").setItems(n,(d,w)->{Intent i=new Intent(this,ClientActivity.class);i.putExtra("id",ds.clients.get(w).id);startActivity(i);}).show();}
    void newReminder(){showReminderDialog();}
    void showHelp(){new AlertDialog.Builder(this).setTitle("Como usar o Devedores").setMessage("1. Cadastre o cliente.\n2. Abra o cliente e adicione fotos/documentos.\n3. Crie o empréstimo.\n4. Registre cada pagamento.\n5. O app avisa dos vencimentos.\n\nTudo é salvo automaticamente no celular.").setPositiveButton("Entendi",null).show();}
    void showReminderDialog(){LinearLayout p=Ui.col(this);p.setPadding(0,0,0,0);EditText t=Ui.field(this,"O que você quer lembrar?");EditText d=Ui.field(this,"Detalhes (opcional)");p.addView(t);Ui.gap(this,p,7);p.addView(d);new AlertDialog.Builder(this).setTitle("Novo lembrete").setView(p).setPositiveButton("Escolher data",(x,w)->pickDateTime(t.getText().toString(),d.getText().toString())).setNegativeButton("Cancelar",null).show();}
    void pickDateTime(String title,String details){Calendar c=Calendar.getInstance();new DatePickerDialog(this,(v,y,m,day)->{Calendar x=Calendar.getInstance();x.set(y,m,day);new TimePickerDialog(this,(tv,h,min)->{x.set(Calendar.HOUR_OF_DAY,h);x.set(Calendar.MINUTE,min);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);Models.Reminder r=new Models.Reminder();r.title=title.trim().isEmpty()?"Lembrete":title.trim();r.details=details;r.when=x.getTimeInMillis();ds.reminders.add(r);ds.save();AlarmScheduler.schedule(this,"reminder",r.id,r.title,r.when);Toast.makeText(this,"Lembrete salvo",Toast.LENGTH_SHORT).show();render();},c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}
    void exportBackup(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"devedores-backup-v5.json");startActivityForResult(i,301);}
    void importBackup(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,302);}
    @Override protected void onActivityResult(int r,int res,Intent data){super.onActivityResult(r,res,data);if(res!=RESULT_OK||data==null)return;try{if(r==301){java.io.OutputStream out=getContentResolver().openOutputStream(data.getData());out.write(ds.exportJson().getBytes("UTF-8"));out.close();Toast.makeText(this,"Backup exportado",Toast.LENGTH_SHORT).show();}else if(r==302){java.io.InputStream in=getContentResolver().openInputStream(data.getData());java.io.ByteArrayOutputStream b=new java.io.ByteArrayOutputStream();byte[] x=new byte[8192];int n;while((n=in.read(x))>0)b.write(x,0,n);in.close();if(ds.importJson(new String(b.toByteArray(),"UTF-8"))){AlarmScheduler.rescheduleAll(this);render();Toast.makeText(this,"Backup restaurado",Toast.LENGTH_SHORT).show();}else Toast.makeText(this,"Backup inválido",Toast.LENGTH_LONG).show();}}catch(Exception e){Toast.makeText(this,"Não foi possível concluir",Toast.LENGTH_LONG).show();}}
}
