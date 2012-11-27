package pl.magot.vetch.ancal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;

import pl.magot.vetch.ancal.database.DataTable;
import pl.magot.vetch.ancal.database.Database;
import pl.magot.vetch.ancal.reminder.AlarmsManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public abstract class CommonActivity extends Activity
{
	
	//Ajout de CHOIX comme mode de demarrage
	public enum StartMode
	{
		Default, EDIT, NEW, VIEW, CHOIX
	};
	
	//enum
	public enum Action
	{
		Default, ShowMsg
	};

	// consts
	public static final String bundleOptionsUpdated = "OptionsUpdated";
	public static final String bundleTableUpdated = "TableUpdated";
	public static final String bundleRowId = "RowId";
	public static final String bundleHourOfDay = "HourOfDay";
	public static final String bundleMinutes = "Minutes";
	public static final String bundleAgendaView = "CurrentAgendaView";
	public static final String bundleAgendaViewStartDate = "AgendaViewStartDate";

	// fields
	private StartMode startMode = StartMode.Default;
	private Bundle bundleDataStartup = new Bundle();
	protected Bundle bundleOtherDataStartup = new Bundle();
	protected Bundle bundleDateValues = new Bundle();

	// fields
	public Prefs prefs = null;
	public Utils utils = null;
	public Database userdb = null;

	protected Bundle freeze = null;

	// Variable gestion fichier
	private File fichiersave = null; // Ajout variable fichier
	
	// methods
	public CommonActivity()
	{
		//Inserer ici lecture du fichier ?
	}

	@Override
	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);

		freeze = null;
		if (icicle != null)
			freeze = new Bundle(icicle);

		// initialize base objects
		prefs = new Prefs(this);
		utils = new Utils(this);
		userdb = new Database(this);
	}

	@Override
	public void onStart()
	{
		super.onStart();
	}

	@Override
	public void onResume()
	{
		super.onResume();

	}

	@Override
	public void onStop()
	{
		super.onStop();

	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		// close database
		userdb.Close();
	}

	public StartMode GetStartMode()
	{
		startMode = StartMode.Default;
		String sAction = getIntent().getAction();
		if (sAction.contains("ACTION_MODE_NEW"))
			startMode = StartMode.NEW;
		if (sAction.contains("ACTION_MODE_EDIT"))
			startMode = StartMode.EDIT;
		if (sAction.contains("ACTION_MODE_VIEW"))
			startMode = StartMode.VIEW;
		
		// On crée un nouveau fichier. Si le fichier existe déjà il ne sera pas crée
		//AJOUT MODE ACCEPTATION/REJET
		//C'est ici qu'il faut faire la verification du fichier texte
		//*
		long datefic=0, datelm=1; //initialisation
		String temp = null;
		BufferedWriter writer = null;
		BufferedReader reader = null;
		File dir = getDir("Fichiers",MODE_PRIVATE);
		fichiersave = new File(dir.getAbsolutePath() + File.separator + "troll.txt");
		
		try
		{
			if(fichiersave.exists())	//si le fichier existe
			{
				datelm = fichiersave.lastModified();	//on recupere la date de derniere modif
				reader = new BufferedReader (new FileReader(fichiersave));
				temp = reader.readLine();	//on lit la premiere ligne du fichier ou est enregistré la derniere date de derniere modification (pour les test = 4/09/1983)
				datefic = Long.parseLong(temp); //simple conversion
			}
			else //si le fichier n'existe pas
			{
				fichiersave.createNewFile(); //creation
				
				try{
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fichiersave,true))); //on ecrit a la fin du fichier
					writer.write("1211580000"); // 23 mai 2008
				}catch (IOException e) {
		      e.printStackTrace();
				}finally {
		      if (writer != null) {
			      try {
			      	writer.close();
			      }catch (IOException e) {
			      	e.printStackTrace();
			      }
		      }
				}
				
				datelm = fichiersave.lastModified();
			}
		} catch (IOException e1)
		{
			e1.printStackTrace();
		}finally {
      if (reader != null) {
        try{
        	reader.close();
        }catch (IOException e) {
        	e.printStackTrace();
        }
      }
		}
		
		if(datefic < datelm){ // Condition = date de modification, /!\ les dates sont au format timestamp (beaucoup plus simple de comparer et de travailler dessus)
			startMode = StartMode.CHOIX;	//Mode de demarage activé si le fichier texte a été mis a jour (mode par defaut durant les test)
		}
		//*/
		return startMode;
	}

	public void SetActivityTitle(String sSubTitle)
	{
		String sTitle = getResources().getString(R.string.app_name);
		if (sSubTitle.length() > 0)
			sTitle += ": " + sSubTitle;
		setTitle(sTitle);
	}

	
	//Fonction OpenActivity a modifier pour lancer l'appel a ActivityAppointment en cas de proposition de rendez-vous
	
	public void OpenActivity(int iActivityRequestCode, String sAction)
	{
		//if(GetStartMode() == StartMode.CHOIX) OpenActivity(0, "android.intent.action.AnCal.ACTION_MODE_NEW_APPOINTMENT",-1);
		OpenActivity(iActivityRequestCode, sAction, -1);
	}

	public void OpenActivity(int iActivityRequestCode, String sAction, long lRowId)
	{
		bundleDataStartup.clear();
		bundleDataStartup.putLong(CommonActivity.bundleRowId, lRowId);
		bundleDataStartup.putAll(bundleOtherDataStartup);
		Intent it = new Intent(sAction);
		it.putExtras(bundleDataStartup);
		startActivityForResult(it, iActivityRequestCode);
	}

	private void ParseBundledAction(Bundle extras)
	{
		if (extras.containsKey("action"))
		{
			Action an = Action.valueOf(extras.getString("action"));

			// check result action of subactivity startup
			if (an == Action.ShowMsg)
			{
				int iMsgId = extras.getInt("msgResStrId");
				int iMsgType = extras.getInt("msgType");
				// show result message
				utils.ShowMsgResStr(iMsgId, iMsgType);
			}
		}
	}

	private Bundle PutBundledMessage(int iMsgId)
	{
		Bundle extras = new Bundle();
		extras.putInt("msgResStrId", iMsgId);
		extras.putInt("msgType", Utils.MSGTYPE_ERROR);
		extras.putString("action", Action.ShowMsg.toString());
		return extras;
	}

	public static Bundle getIntentExtras(Intent data)
	{
		// data is null, when activity cancelled by BACK BUTTON
		if (data != null)
		{
			Bundle extras = data.getExtras();
			if (extras != null)
			{
				return extras;
			}
		}
		return null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		Bundle extras = getIntentExtras(data);
		if (extras != null)
		{
			if (resultCode == RESULT_CANCELED)
			{
				ParseBundledAction(extras);
			}
			if (resultCode == RESULT_OK)
			{
				// OK RESULT parsed in parent class
			}
		}
	}

	protected void setIntentResult(String action, int resultCode, Bundle bundle)
	{
		Intent intentData = new Intent(action);
		intentData.putExtras(bundle);
		setResult(resultCode, intentData);
	}

	public boolean OpenDataForEdit(DataTable data)
	{
		long lRowId = RequestedRowId();
		Database.Result result = data.GetRowDataForEdit(lRowId);
		if (result == Database.Result.Success)
		{
			// save date values for change test
			SaveDateValuesBeforeChange(bundleDateValues);

			return true;
		} else
		{
			// return fail result for caller
			int iMsgId = Database.GetErrDesc(result);
			Bundle extras = PutBundledMessage(iMsgId);
			setIntentResult("", RESULT_CANCELED, extras);

			return false;
		}
	}

	public long RequestedRowId()
	{
		return getIntent().getExtras().getLong(CommonActivity.bundleRowId);
	}

	public boolean SaveDataToTable(DataTable data)
	{
		boolean bSuccess = false;
		// update data into database
		if (data.GetDataRow().Validate())
		{
			boolean bInsertMode = (GetStartMode() == StartMode.NEW || GetStartMode() == StartMode.CHOIX); // Le mode CHOIX corespond a un premier enregistrement dans la table
			long lRowId = RequestedRowId();
			Database.Result result = data.UpdateData(bInsertMode, lRowId);
			if (result == Database.Result.Success)
			{
				// if important dates changed, reset alarm
				if (DateValuesChanged(bundleDateValues))
					AlarmsManager.ResetAlarm(userdb, prefs, data, lRowId);

				bSuccess = true;
			} else
			{
				utils.ShowMsgResStr(Database.GetErrDesc(result), Utils.MSGTYPE_ERROR);
			}
		} else
		{
			utils
					.ShowMsgResStr(R.string.infoEnterAllRequiredData, Utils.MSGTYPE_INFO);
		}
		return bSuccess;
	}

	public boolean DateBeforeNow(Calendar calDate)
	{
		Calendar calNow = Calendar.getInstance();
		if (calNow.get(Calendar.YEAR) == calDate.get(Calendar.YEAR))
			if (calNow.get(Calendar.MONTH) == calDate.get(Calendar.MONTH))
				if (calNow.get(Calendar.DAY_OF_MONTH) == calDate
						.get(Calendar.DAY_OF_MONTH))
					return false;
		if (calDate.before(calNow))
		{
			utils.ShowMsgResStr(R.string.infoEnterValidDate, Utils.MSGTYPE_INFO);
			return true;
		}
		return false;
	}

	public void CloseActivity()	//Ajout d'un nouveau prototype a cette fonction, de facon a ce que lors du rejet d'un rendez vous, la page se ferme (a modifier pour suprimer le rendez vous du fichier texte)
	{
		Bundle bundleDataResult = new Bundle();
		setIntentResult("", RESULT_OK, bundleDataResult);
		finish();
	}
	
	public void CloseActivity(String sCode, String sValue)
	{
		Bundle bundleDataResult = new Bundle();
		bundleDataResult.putString(sCode, sValue);

		setIntentResult("", RESULT_OK, bundleDataResult);
		finish();
	}

	public void CloseActivity(DataTable data)
	{
		Bundle bundleDataResult = new Bundle();
		bundleDataResult.putString(bundleTableUpdated, data.GetTableName());

		setIntentResult("", RESULT_OK, bundleDataResult);
		finish();
	}

	public boolean DeleteDataFromTable(DataTable data)
	{
		boolean bSuccess = false;
		long lRowId = RequestedRowId();
		Database.Result result = data.DeleteData(lRowId);
		if (result == Database.Result.Success)
		{
			// check if alarm for delete and delete it
			AlarmsManager.DeleteAlarm(userdb, prefs, data, lRowId);

			bSuccess = true;
		} else
		{
			utils.ShowMsgResStr(Database.GetErrDesc(result), Utils.MSGTYPE_ERROR);
		}
		return bSuccess;
	}

	public void SaveDateValuesBeforeChange(Bundle data)
	{
		if (data != null)
			data.clear();
	}

	public boolean DateValuesChanged(Bundle data)
	{
		return false;
	}

	protected void restoreStateFromFreezeIfRequired()
	{
		if (freeze != null)
		{
			restoreStateFromFreeze();
		}
	}

	abstract protected void restoreStateFromFreeze();

}
