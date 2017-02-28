package commandLineMenus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import commandLineMenus.depthFirstSearch.*;
import commandLineMenus.rendering.examples.*;
import commandLineMenus.rendering.*;
import commandLineMenus.util.InOut;

/**
 * Menu affiché en ligne de commande. En haut du menu est affiché le {@link titre}, 
 * suivi par une liste d'options. L'utilisateur est invité à choisir une option
 * qui est ensuite exécutée. Il est possible de placer un sous-menu en option, 
 * ou il est possible d'utiliser la classe Option pour affecter une action à la sélection 
 * d'une action.
 */

public class Menu extends Option
{
	private Map<String, Option> optionsMap = new TreeMap<>();
	private List<Option> optionsList = new ArrayList<>();
	private boolean autoBack = false;
	private String shortTitle;
	
	/**
	 * Créée un menu.
	 * @param title titre affiché au dessus du menu.
	 */
	
	public Menu(String title)
	{
		this(title, null);
		shortTitle = title;
	}
	
	/**
	 * Créée un menu.
	 * @param title titre affiché au dessus du menu.
	 * @param shorcut Si ce menu est aussi une option, 
	 * raccourci permettant de l'activer.
	 */
	
	public Menu(String title, String shorcut)
	{
		super(title, shorcut);
	}

	/**
	 * Créée un menu.
	 * @param longTitle titre affiché au dessus du menu.
	 * @param shortTitle titre affiché en tant qu'élément de menu (ou en tant qu'option).
	 * @param shortcut Si ce menu est aussi une option, 
	 * raccourci permettant de l'activer.
	 */
	
	public Menu(String longTitle, String shortTitle, String shortcut)
	{
		super(longTitle, shortcut);
		this.shortTitle = shortTitle; 
	}

	private void checkConcurrentModification(String message)
	{
		if (isLocked())
			throw new ConcurrentModificationException(message);		
	}
	
	/**
	 * Ajoute une option dans le menu.
	 * @param option option à ajouter.
	 */
	
	public void add(Option option)
	{
		checkConcurrentModification("Impossible to add option \""
					+ option.getTitle() + "\" while running.");
		String raccourci = option.getShorcut();
		if (raccourci == null)
			throw new ShortcutMissingException(option);
		Option autre = optionsMap.get(raccourci);
		if (autre != null)
			throw new CollisionException(autre, option);
		optionsMap.put(option.getShorcut(), option);
		optionsList.add(option);
	}
	
	public Set<Option> getOptions()
	{
		return new HashSet<>(optionsMap.values());
	}
	
	public int size()
	{
		return optionsList.size();
	}
	
	protected void clearOptions()
	{
		optionsList.clear();
		optionsMap.clear();
	}
	
	protected void setRenderers(MenuRenderer menuRenderer)
	{
		super.setRenderers(menuRenderer);
		for (Option option : getOptions())
			option.setRenderers(this.menuRenderer);
	}
	
	/**
	 * Ajoute une option permettant de quitter le programme.
	 * @param shorcut le raccourci permettant de quitter le programme.
	 */
	
	public void addQuit(String shorcut)
	{
		add(new Option("Exit", shorcut, Action.QUIT));
	}
	
	/**
	 * Ajoute une option permettant de revenir au menu précédent.
	 * @param shorcut le raccourci permettant de revenir au menu précédent.
	 */
	
	public void addBack(String shorcut)
	{
		add(new Option("Back", shorcut, Action.BACK));
	}
	
	/**
	 * Détermine si le choix d'une option entraîne automatiquement le retour au menu précédent.
	 * Faux par défaut.
	 * @param autoBack vrai ssi si le choix d'une option entraîne le retour au 
	 * menu précédent.
	 */
	
	public void setAutoBack(boolean autoBack)
	{
		checkConcurrentModification("Impossible to change autoBack while running.");
		this.autoBack = autoBack;
	}
	
	protected String getOption()
	{
		System.out.print(this.toString());
		while(true)
		{
			try
			{
				return InOut.getString();				
			}
			catch (IOException e) 
			{
				System.out.println(menuRenderer.invalidInput(""));
			}
		}
	}
	
	/**
	 * Exécute le menu.	Attention, il n'est autorisé de lancer qu'un menu à la fois. Si vous avez besoin de lancer
	 * un menu dans un sous-menu, lisez la documentation du package pour voir comment procéder. 
	 */
	
	public void start()
	{
		DepthFirstSearch.dephtFirstSearch(this);
		if (isLocked())
			throw new ConcurrentExecutionException();
		setRenderers(new MenuDefaultRenderer());
		lock();
		run();
		unlock();
	}

	protected void run()
	{
		if (getOptions().size() == 0)
			throw new EmptyMenuException();
		Option option = null;
		boolean between = false;
		do
		{
			if (between)
				System.out.println(menuRenderer.betweenMenus());
			else
				between = true;
			String get = getOption();
			option = optionsMap.get(get);
			if (option != null)
				option.optionSelected();
			else
				System.out.println(menuRenderer.invalidInput(get));
		}
		while(option == null || !autoBack && option.getAction() != Action.BACK);
	}

	@Override
	void optionSelected()
	{
		this.run();
	}
	
	@Override
	public String stringOfOption()
	{
		if (shortTitle != null)
			return menuRenderer.option(shortcut, shortTitle);
		else
			return super.stringOfOption();
	}
	
	private String emptyIfNull(String s)
	{
		if (s != null)
			return s;
		return "";
	}
	
	@Override
	public String toString()
	{
		String res = menuRenderer.header(getTitle());
		boolean between = false;
		for (Option option : optionsList)
		{
			if (!between) 
				res += menuRenderer.betweenOptions();
			else
				between = true;
			res += option.stringOfOption();
		}
		res += emptyIfNull(menuRenderer.footer());
		res += emptyIfNull(menuRenderer.prompt());
		return res;
	}

	class CollisionException extends RuntimeException
	{
		private static final long serialVersionUID = 1142845287292812411L;
		private Option oldOption, newOption;
		
		public Option getOldOption()
		{
			return oldOption;
		}
		
		public Option getNewOption()
		{
			return newOption;
		}
		
		CollisionException(Option oldOption, Option newOption)
		{
			super("Collision between " + oldOption.getTitle()
				+ " and " + newOption.getTitle() + " for the shorcut "
				+ newOption.getShorcut() + " in the menu " + 
			getTitle() + ".");
			this.oldOption = oldOption;
			this.newOption = newOption;
		}
	}

	public class ShortcutMissingException extends RuntimeException
	{
		private static final long serialVersionUID = -194430644006701341L;
		private Option option;
		
		public Option getOption()
		{
			return option;
		}
		
		ShortcutMissingException(Option option)
		{
			super("Impossible to add option \"" + option.getTitle() 
				+ "\" in the menu \"" + getTitle() + "\" if the shorcut hasn't been specified.");
			this.option = option;
		}
	}
	
	public class EmptyMenuException extends RuntimeException
	{
		private static final long serialVersionUID = 3617589300605854823L;

		public EmptyMenuException() 
		{
			super("Impossible to launch the empty menu \"" + getTitle() + "\".");
		}
	}

	public class ConcurrentExecutionException extends RuntimeException
	{
		private static final long serialVersionUID = 770804726891062420L;

		public ConcurrentExecutionException() 
		{
			super("Impossible to launch \"" + getTitle() 
				+ "\", a menu application is already running.");
		}
	}
	
	public static class CycleDetectedException extends RuntimeException
	{
		private static final long serialVersionUID = -2884917321791851520L;

		public CycleDetectedException(LinkedList<Menu> cycleDetected)
		{
			super("A directed cycle has been detected in the menu tree :\n" 
					+ stringOfCycle(cycleDetected));
		}
		
		static private String stringOfCycle(List<Menu> list)
		{
			String res = "";
			boolean first = true;
			for (Menu menu : list)
			{
				if (!first)
					res += " -> ";
				else
					first = false;
				res += menu.getTitle();
			}
			return res;
		}
	}
}