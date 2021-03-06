/**
 * 
 */
package net.fexcraft.app.fmt.porters;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import net.fexcraft.app.fmt.FMTB;
import net.fexcraft.app.fmt.ui.UserInterface;
import net.fexcraft.app.fmt.ui.general.DialogBox;
import net.fexcraft.app.fmt.ui.general.FileChooser.AfterTask;
import net.fexcraft.app.fmt.ui.general.FileChooser.ChooserMode;
import net.fexcraft.app.fmt.ui.general.FileChooser.FileRoot;
import net.fexcraft.app.fmt.utils.SaveLoad;
import net.fexcraft.app.fmt.utils.Settings.Setting;
import net.fexcraft.app.fmt.utils.Translator;
import net.fexcraft.app.fmt.wrappers.GroupCompound;
import net.fexcraft.app.fmt.wrappers.TurboList;
import net.fexcraft.lib.common.json.JsonUtil;

/**
 * @author Ferdinand Calo' (FEX___96)
 *
 */
public class PorterManager {
	
	private static final PorterMap porters = new PorterMap();
	
	public static final void load() throws NoSuchMethodException, FileNotFoundException, ScriptException {
		File root = new File("./resources/porters"); porters.clear();
		if(!root.exists()){ root.mkdirs(); }
		else{
			for(File file : root.listFiles()){
				if(file.getName().endsWith(".js")){
					try{
						ScriptEngine engine = newEngine(); engine.eval(new FileReader(file)); Invocable inv = (Invocable)engine;
						//
						ExternalPorter porter = new ExternalPorter(); porter.file = file;
						porter.id = (String)inv.invokeFunction("getId");
						porter.name = (String)inv.invokeFunction("getName");
						porter.extensions = ((ScriptObjectMirror)inv.invokeFunction("getExtensions")).to(String[].class);
						porter.importer = (boolean)inv.invokeFunction("isImporter");
						porter.exporter = (boolean)inv.invokeFunction("isExporter");
						porter.settings = new ArrayList<>();//TODO
						porters.put(porter.id, porter);
					}
					catch(Exception e){
						e.printStackTrace(); System.exit(1);
					}
				}
			}
		}
		//
		porters.add(new MTBImporter());
		porters.add(new FVTMExporter());
		porters.add(new OBJPreviewImporter());
		porters.add(new JTMTPorter());
		porters.add(new PNGExporter());
		porters.add(new OBJPrototypeExporter());
		porters.add(new MarkerExporter());
		porters.add(new TiMExporter());
	}

	private static ScriptEngine newEngine(){
		return new ScriptEngineManager().getEngineByName("nashorn");
	}

	public static void handleImport(){
		UserInterface.FILECHOOSER.show(new String[]{ Translator.translate("filechooser.import.title", "Select file/model to import."),
			Translator.translate("filechooser.import.confirm", "Import") }, FileRoot.IMPORT, new AfterTask(){
			@Override
			public void run(){
				try{
					if(file == null){
						FMTB.showDialogbox(Translator.translate("dialog.import.nofile", "No valid file choosen.<nl>Import is cancelled."),
							Translator.translate("dialog.import.nofile.confirm", "ok.."), null, DialogBox.NOTHING, null);
						return;
					}
					GroupCompound compound = null;
					if(porter.isInternal()){
						compound = ((InternalPorter)porter).importModel(file, mapped_settings);
					}
					else{
						Invocable inv = (Invocable)((ExternalPorter)porter).eval();
						String result = (String) inv.invokeFunction("importModel", file);
						compound = SaveLoad.parseModel(file, JsonUtil.getObjectFromString(result));
					}
					if(mapped_settings.get("integrate").getBooleanValue()){
						for(String creator : compound.creators){
							if(!FMTB.MODEL.creators.contains(creator)){
								FMTB.MODEL.creators.add(creator);
							}
						}
						for(TurboList list : compound.getGroups()){
							String name = compound.name + "_" + list.id;
							while(FMTB.MODEL.getGroups().contains(name)){
								name += "_"; if(name.length() > 64) break;
							}
							FMTB.MODEL.getGroups().add(list);
						}
					}
					else FMTB.MODEL = compound;
					FMTB.MODEL.updateFields(); FMTB.MODEL.recompile();
				}
				catch(Exception e){
					String str = Translator.format("dialog.import.fail", "Errors while importing Model.<nl>%s", e.getLocalizedMessage());
					FMTB.showDialogbox(str, Translator.translate("dialog.import.fail.confirm", "ok."), null, DialogBox.NOTHING, null);//TODO add "open console" as 2nd button
					e.printStackTrace();
				}
				FMTB.showDialogbox(Translator.translate("dialog.import.success", "Import complete."), Translator.translate("dialog.import.success.confirm", "OK!"), null, DialogBox.NOTHING, null);
			}
		}, ChooserMode.IMPORT);
	}

	public static void handleExport(){
		UserInterface.FILECHOOSER.show(new String[]{ Translator.translate("filechooser.export.title", "Select Export Location"),
			Translator.translate("filechooser.export.confirm", "Export") }, FileRoot.EXPORT, new AfterTask(){
			@Override
			public void run(){
				try{
					if(file == null){
						FMTB.showDialogbox(Translator.translate("dialog.export.nofile", "No valid file choosen.<nl>Export is cancelled."),
							Translator.translate("dialog.export.nofile.confirm", "ok.."), null, DialogBox.NOTHING, null);
						return;
					} String result;
					if(porter.isInternal()){
						result = ((InternalPorter)porter).exportModel(FMTB.MODEL, file, mapped_settings);
					}
					else{
						Invocable inv = (Invocable)((ExternalPorter)porter).eval();
						result = (String)inv.invokeFunction("exportModel", SaveLoad.modelToJTMT(null, true).toString(), file);
					}
					FMTB.showDialogbox(Translator.format("dialog.export.success", "Export complete.<nl>%s", result),
						Translator.translate("dialog.export.success.confirm", "OK!"), null, DialogBox.NOTHING, null);
					Desktop.getDesktop().open(file.getParentFile());
				}
				catch(Exception e){
					String str = Translator.format("dialog.export.fail", "Errors while exporting Model.<nl>%s", e.getLocalizedMessage());
					FMTB.showDialogbox(str, Translator.translate("dialog.export.fail.confirm", "ok."), null, DialogBox.NOTHING, null);//TODO add "open console" as 2nd button
					e.printStackTrace();
				}
			}
		}, ChooserMode.EXPORT);
	}

	/**
	 * @param file
	 * @return porter compatible with this file extension
	 */
	public static ExImPorter getPorterFor(File file, boolean export){
		for(ExImPorter porter : porters.values()){
			if((export && porter.isExporter()) || (!export && porter.isImporter())){
				for(String ext : porter.getExtensions()){
					if(file.getName().endsWith(ext)) return porter;
				}
			}
		}
		return null;
	}
	
	public static class ExternalPorter extends ExImPorter {

		private File file;
		public String id, name;
		public String[] extensions;
		public boolean importer, exporter;
		private ArrayList<Setting> settings;
		
		/**
		 * @return new ScriptEngine instance with this porter loaded
		 * @throws ScriptException 
		 * @throws FileNotFoundException 
		 */
		public ScriptEngine eval() throws FileNotFoundException, ScriptException{
			ScriptEngine engine = newEngine();
			engine.eval(new FileReader(file));
			return engine;
		}
		
		@Override
		public boolean isInternal(){ return false; }
		
		@Override
		public String getId(){ return id; }
		
		@Override
		public String getName(){ return name; }
		
		@Override
		public String[] getExtensions(){ return extensions; }
		
		@Override
		public boolean isImporter(){ return importer; }
		
		@Override
		public boolean isExporter(){ return exporter; }

		@Override
		public ArrayList<Setting> getSettings(boolean export){
			return settings;
		}
		
	}
	
	public static abstract class ExImPorter {
		
		public abstract String getId();
		
		public abstract String getName();
		
		public abstract String[] getExtensions();
		
		public abstract boolean isImporter();
		
		public abstract boolean isExporter();
		
		public abstract boolean isInternal();
		
		public boolean isValidFile(File pre){
			if(pre.isDirectory()) return true;
			for(String str : this.getExtensions())
				if(pre.getName().endsWith(str)) return true;
			return false;
		}

		public abstract List<Setting> getSettings(boolean export);
		
	}
	
	public static abstract class InternalPorter extends ExImPorter {
		
		protected static final List<Setting> nosettings = Collections.unmodifiableList(new ArrayList<>());
		
		/** @return new groupcompound based on data in the file */
		public abstract GroupCompound importModel(File file, Map<String, Setting> settings);
		
		/** @return result/status text; */
		public abstract String exportModel(GroupCompound compound, File file, Map<String, Setting> settings);
		
		@Override
		public boolean isInternal(){ return true; }
		
	}

	/**
	 * @return
	 */
	public static List<ExImPorter> getPorters(boolean export){
		return porters.values().stream().filter(pre -> export ? pre.isExporter() : pre.isImporter()).collect(Collectors.<ExImPorter>toList());
	}
	
	private static class PorterMap extends TreeMap<String, ExImPorter> {
		
		private static final long serialVersionUID = 1L;

		public void add(ExImPorter porter){
			this.put(porter.getId(), porter);
		}
		
	}

}
