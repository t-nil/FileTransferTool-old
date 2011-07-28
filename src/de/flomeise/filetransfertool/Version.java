package de.flomeise.filetransfertool;

/**
 * A simple representation of a program version
 * @author Flohw
 */
public final class Version implements Comparable {
	private final int first;
	private final int second;
	private final int third;
	private final int revision;

	/**
	 * Creates a new version with the three version numbers specified
	 * @param first the first version number
	 * @param second the second version number
	 * @param third the second version number
	 */
	public Version(int first, int second, int third, int revision) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.revision = revision;
	}

	/**
	 * Creates a new version by parsing the given string with the scheme &lt;first&rt;.&lt;second&rt;.&lt;third&rt;
	 * @param s the string to be parsed
	 */
	public Version(String s) {
		this.first = Integer.parseInt(s.split("\\.")[0]);
		this.second = Integer.parseInt(s.split("\\.")[1]);
		this.third = Integer.parseInt(s.split("\\.")[2].split("\\-")[0]);
		this.revision = Integer.parseInt(s.split("\\-")[1]);
	}

	/**
	 * @return the first
	 */
	public int getFirst() {
		return first;
	}

	/**
	 * @return the second
	 */
	public int getSecond() {
		return second;
	}

	/**
	 * @return the third
	 */
	public int getThird() {
		return third;
	}
	
	public int getRevision() {
		return revision;
	}

	@Override
	public String toString() {
		return first + "." + second + "." + third + "-" +  revision;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || !(o instanceof Version))
			return false;
		
		if(o == this) {
			return true;
		}


		Version v = (Version) o;
		return first == v.getFirst()
			   && second == v.getSecond()
			   && third == v.getThird()
			   && revision == v.getRevision();
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 83 * hash + this.first;
		hash = 83 * hash + this.second;
		hash = 83 * hash + this.third;
		hash = 83 * hash + this.revision;
		return hash;
	}

	@Override
	public int compareTo(Object o) {
		if(this == o) {
			return 0;
		}

		if(o instanceof Version) {
			Version v = (Version) o;
			if(first == v.getFirst()) {
				if(second == v.getSecond()) {
					if(third == v.getThird()) {
						if(revision == v.getRevision()) {
							return 0;
						} else if(revision > v.getRevision()) {
							return 1;
						} else {
							return -1;
						}
					} else if(third > v.getThird()) {
						return 1;
					} else {
						return -1;
					}
				} else if(second > v.getSecond()) {
					return 1;
				} else {
					return -1;
				}
			} else if(first > v.getFirst()) {
				return 1;
			} else {
				return -1;
			}
		} else {
			throw new ClassCastException(o.getClass().getCanonicalName() + " cannot be compared to " + this.getClass().getCanonicalName());
		}
	}
}