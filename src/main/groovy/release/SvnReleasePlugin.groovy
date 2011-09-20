package release

import java.util.regex.Matcher
import org.gradle.api.GradleException

/**
 * A command-line style SVN client. Requires user has SVN installed locally.
 * @author elberry
 * @author evgenyg
 * Created: Tue Aug 09 23:25:18 PDT 2011
 */
// TODO: Use SVNKit or SubversionJ
class SvnReleasePlugin extends BaseScmPlugin {

    private static final String ERROR = 'Commit failed'

    void init () {
        findSvnUrl()
        project.convention.plugins.SvnReleasePlugin = new SvnReleasePluginConvention()
    }


    void checkCommitNeeded () {
        String out = exec( 'svn', 'status' )
        def changes = 0
        def unknown = 0
        out.eachLine { line ->
            switch (line?.trim()?.charAt(0)) {
                case '?':
                    unknown++
                    break
                default:
                    changes++
                    break
            }
        }
        if (changes) {
            throw new GradleException('You have un-committed changes.')
        }
        if ( releaseConvention().failOnUnversionedFiles && unknown) {
            throw new GradleException('You have un-versioned files.')
        }
    }


    void checkUpdateNeeded () {
            // svn status -q -u
            String out = exec( 'svn', 'status', '-q', '-u' )
            def missing = 0
            out.eachLine { line ->
                switch (line?.trim()?.charAt(0)) {
                    case '*':
                        missing++
                        break
                }
            }
            if (missing) {
                throw new GradleException("You are missing $missing changes.")
            }
    }


    void createReleaseTag () {
        def    props   = project.properties
        String svnUrl  = props.releaseSvnUrl
        String svnRev  = props.releaseSvnRev
        String svnRoot = props.releaseSvnRoot
        String svnTag  = props.version

        exec( 'svn', 'cp', "${svnUrl}@${svnRev}", "${svnRoot}/tags/${svnTag}", '-m', 'v' + svnTag )
    }


    void commit( String message ) {
        exec( [ 'svn', 'ci', '-m', message ], 'Error committing new version', ERROR )
    }


    private void findSvnUrl() {
        String out        = exec( 'svn', 'info' )
        def    urlPattern = ~/URL:\s(.*?)(\/(trunk|branches|tags).*?)$/
        def    revPattern = ~/Revision:\s(.*?)$/

        out.eachLine { line ->
            Matcher matcher = line =~ urlPattern
            if (matcher.matches()) {
                String svnRoot = matcher.group(1)
                String svnProject = matcher.group(2)
                project.setProperty('releaseSvnRoot', svnRoot)
                project.setProperty('releaseSvnUrl', "$svnRoot$svnProject")
            }
            matcher = line =~ revPattern
            if (matcher.matches()) {
                String revision = matcher.group(1)
                project.setProperty('releaseSvnRev', revision)
            }
        }
        if (!project.hasProperty('releaseSvnUrl')) {
            throw new GradleException('Could not determine root SVN url.')
        }
    }
}