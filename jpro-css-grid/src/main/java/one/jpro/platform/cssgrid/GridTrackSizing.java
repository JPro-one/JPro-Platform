package one.jpro.platform.cssgrid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * CSS Grid track sizing algorithm (CSS Grid Layout Level 1, §12) for one axis.
 */
final class GridTrackSizing {

    private static final double INF = Double.POSITIVE_INFINITY;
    private static final double EPS = 1e-6;

    /** A concrete track after expanding repeat() groups. */
    static final class Track {
        final GridTrack min;   // FIXED, AUTO, MIN_CONTENT or MAX_CONTENT
        final GridTrack max;   // FIXED, FLEX, AUTO, MIN_CONTENT or MAX_CONTENT
        final boolean autoFit;
        boolean collapsed;
        double base;
        double limit;

        Track(GridTrack min, GridTrack max, boolean autoFit) {
            this.min = min;
            this.max = max;
            this.autoFit = autoFit;
        }

        boolean isFlex() { return max.getKind() == GridTrack.Kind.FLEX; }
        double flexFactor() { return max.getValue(); }
        boolean hasIntrinsicMin() { return min.getKind() != GridTrack.Kind.FIXED; }
        boolean hasIntrinsicMax() { return !isFlex() && max.getKind() != GridTrack.Kind.FIXED; }
        boolean isAutoMax() { return max.getKind() == GridTrack.Kind.AUTO; }
    }

    /** An item's extent on this axis together with its content contributions (including margins). */
    static final class Contributor {
        final int start;
        final int span;
        final double minContribution;
        final double maxContribution;

        Contributor(int start, int span, double minContribution, double maxContribution) {
            this.start = start;
            this.span = span;
            this.minContribution = minContribution;
            this.maxContribution = maxContribution;
        }

        int end() { return start + span; }
    }

    private GridTrackSizing() {
    }

    // ── Expansion of track lists ──────────────────────────────────────

    /**
     * Expands a template into concrete tracks, resolving percentages and auto-fill/auto-fit repetitions.
     *
     * @param available content size on this axis, or NaN if indefinite
     */
    static List<Track> expand(GridTrackList template, double available, double gap) {
        List<Track> result = new ArrayList<>();
        for (GridTrack entry : template.getTracks()) {
            if (entry.getKind() != GridTrack.Kind.REPEAT) {
                result.add(toTrack(entry, available, false));
            } else if (entry.getRepeatMode() == GridTrack.RepeatMode.COUNT) {
                for (int i = 0; i < entry.getRepeatCount(); i++) {
                    for (GridTrack t : entry.getRepeatedTracks()) result.add(toTrack(t, available, false));
                }
            } else {
                int count = autoRepeatCount(template, entry, available, gap);
                boolean autoFit = entry.getRepeatMode() == GridTrack.RepeatMode.AUTO_FIT;
                for (int i = 0; i < count; i++) {
                    for (GridTrack t : entry.getRepeatedTracks()) result.add(toTrack(t, available, autoFit));
                }
            }
        }
        return result;
    }

    /** Creates the implicit track with the given implicit index, cycling through {@code autoTracks}. */
    static Track implicitTrack(GridTrackList autoTracks, int implicitIndex, double available) {
        List<GridTrack> list = autoTracks.isEmpty() ? GridTrackList.AUTO.getTracks() : autoTracks.getTracks();
        List<GridTrack> flat = new ArrayList<>();
        for (GridTrack t : list) {
            if (t.getKind() == GridTrack.Kind.REPEAT) {
                int n = Math.max(1, t.getRepeatCount());
                for (int i = 0; i < n; i++) flat.addAll(t.getRepeatedTracks());
            } else {
                flat.add(t);
            }
        }
        int idx = Math.floorMod(implicitIndex, flat.size());
        return toTrack(flat.get(idx), available, false);
    }

    private static Track toTrack(GridTrack t, double available, boolean autoFit) {
        return new Track(resolveSizing(t.minSizing(), available), resolveSizing(t.maxSizing(), available), autoFit);
    }

    private static GridTrack resolveSizing(GridTrack fn, double available) {
        if (fn.getKind() == GridTrack.Kind.PERCENT) {
            return Double.isNaN(available) ? GridTrack.auto() : GridTrack.px(Math.max(0, available) * fn.getValue() / 100.0);
        }
        return fn;
    }

    private static int autoRepeatCount(GridTrackList template, GridTrack repeat, double available, double gap) {
        if (Double.isNaN(available)) return 1;
        double otherSum = 0;
        int otherCount = 0;
        for (GridTrack entry : template.getTracks()) {
            if (entry == repeat) continue;
            if (entry.getKind() == GridTrack.Kind.REPEAT) {
                for (int i = 0; i < entry.getRepeatCount(); i++) {
                    for (GridTrack t : entry.getRepeatedTracks()) {
                        otherSum += definiteSize(t, available);
                        otherCount++;
                    }
                }
            } else {
                otherSum += definiteSize(entry, available);
                otherCount++;
            }
        }
        double unit = 0;
        int unitCount = repeat.getRepeatedTracks().size();
        for (GridTrack t : repeat.getRepeatedTracks()) unit += definiteSize(t, available);
        double denominator = unit + gap * unitCount;
        if (denominator <= 0) return 1;
        double space = available - otherSum - gap * otherCount + gap;
        int count = (int) Math.floor((space + EPS) / denominator);
        return Math.max(1, count);
    }

    /** Size used for auto-repeat counting: the definite max if any, else the definite min, else 0. */
    private static double definiteSize(GridTrack t, double available) {
        GridTrack max = resolveSizing(t.maxSizing(), available);
        if (max.getKind() == GridTrack.Kind.FIXED) return max.getValue();
        GridTrack min = resolveSizing(t.minSizing(), available);
        if (min.getKind() == GridTrack.Kind.FIXED) return min.getValue();
        return 0;
    }

    // ── Sizing ────────────────────────────────────────────────────────

    /**
     * Runs the track sizing algorithm; the result is stored in each track's {@link Track#base}.
     *
     * @param available  content size on this axis, or NaN if indefinite
     * @param gap        gap between tracks
     * @param stretch    whether free space is distributed to {@code auto} tracks (content alignment {@code stretch})
     * @param minContent when the size is indefinite: size under a min-content constraint (tracks stay at their base
     *                   sizes) instead of a max-content constraint (tracks grow to their limits)
     */
    static void size(List<Track> tracks, List<Contributor> items, double available, double gap, boolean stretch, boolean minContent) {
        boolean definite = !Double.isNaN(available);

        // 1. Initialize
        for (Track t : tracks) {
            if (t.collapsed) {
                t.base = 0;
                t.limit = 0;
                continue;
            }
            t.base = t.min.getKind() == GridTrack.Kind.FIXED ? t.min.getValue() : 0;
            t.limit = t.max.getKind() == GridTrack.Kind.FIXED ? t.max.getValue() : INF;
            if (t.limit < t.base) t.limit = t.base;
        }

        // 2. Resolve intrinsic sizes: single-span items
        for (Contributor item : items) {
            if (item.span != 1) continue;
            Track t = tracks.get(item.start);
            if (t.collapsed) continue;
            if (t.hasIntrinsicMin()) {
                double c = t.min.getKind() == GridTrack.Kind.MAX_CONTENT ? item.maxContribution : item.minContribution;
                t.base = Math.max(t.base, c);
            }
            if (t.hasIntrinsicMax()) {
                double c = t.max.getKind() == GridTrack.Kind.MIN_CONTENT ? item.minContribution : item.maxContribution;
                t.limit = t.limit == INF ? c : Math.max(t.limit, c);
            }
            if (t.limit < t.base) t.limit = t.base;
        }

        // 2b. Spanning items that do not cross flexible tracks, in groups of ascending span
        List<Contributor> spanning = new ArrayList<>();
        for (Contributor item : items) if (item.span > 1) spanning.add(item);
        spanning.sort(Comparator.comparingInt(c -> c.span));
        int groupStart = 0;
        while (groupStart < spanning.size()) {
            int groupEnd = groupStart;
            while (groupEnd < spanning.size() && spanning.get(groupEnd).span == spanning.get(groupStart).span) groupEnd++;
            List<Contributor> group = new ArrayList<>();
            for (Contributor item : spanning.subList(groupStart, groupEnd)) {
                if (!spansFlex(tracks, item)) group.add(item);
            }
            // intrinsic minimums, max-content minimums, intrinsic maximums, max-content maximums
            distributeSpanGroup(tracks, group, gap, false, false, Track::hasIntrinsicMin, Track::hasIntrinsicMax);
            distributeSpanGroup(tracks, group, gap, false, true,
                    t -> t.min.getKind() == GridTrack.Kind.MAX_CONTENT, t -> t.max.getKind() == GridTrack.Kind.MAX_CONTENT);
            distributeSpanGroup(tracks, group, gap, true, false, Track::hasIntrinsicMax, null);
            distributeSpanGroup(tracks, group, gap, true, true,
                    t -> t.hasIntrinsicMax() && t.max.getKind() != GridTrack.Kind.MIN_CONTENT, null);
            groupStart = groupEnd;
        }

        // 2c. Spanning items crossing flexible tracks: their minimum goes to the flexible tracks
        for (Contributor item : spanning) {
            if (!spansFlex(tracks, item)) continue;
            double extra = item.minContribution - spannedSize(tracks, item, gap);
            if (extra <= EPS) continue;
            double factorSum = 0;
            int flexCount = 0;
            for (int i = item.start; i < item.end(); i++) {
                Track t = tracks.get(i);
                if (t.isFlex() && !t.collapsed) { factorSum += t.flexFactor(); flexCount++; }
            }
            for (int i = item.start; i < item.end(); i++) {
                Track t = tracks.get(i);
                if (!t.isFlex() || t.collapsed) continue;
                t.base += factorSum > 0 ? extra * t.flexFactor() / factorSum : extra / flexCount;
                if (t.limit < t.base) t.limit = t.base;
            }
        }

        // 2d. Remaining infinite growth limits become the base size
        for (Track t : tracks) if (t.limit == INF) t.limit = t.base;

        // 3. Maximize tracks (under a max-content constraint the free space is infinite)
        if (!definite) {
            if (!minContent) {
                for (Track t : tracks) t.base = t.limit;
            }
        } else {
            double free = available - gapsTotal(tracks, gap) - baseSum(tracks);
            if (free > EPS) {
                List<Track> growable = new ArrayList<>();
                for (Track t : tracks) if (!t.collapsed && t.limit > t.base + EPS) growable.add(t);
                while (free > EPS && !growable.isEmpty()) {
                    double share = free / growable.size();
                    for (int i = growable.size() - 1; i >= 0; i--) {
                        Track t = growable.get(i);
                        double add = Math.min(share, t.limit - t.base);
                        t.base += add;
                        free -= add;
                        if (t.limit - t.base <= EPS) growable.remove(i);
                    }
                }
            }
        }

        // 4. Expand flexible tracks (the flex fraction is zero under a min-content constraint)
        List<Track> flexTracks = new ArrayList<>();
        for (Track t : tracks) if (t.isFlex() && !t.collapsed) flexTracks.add(t);
        if (!flexTracks.isEmpty() && (definite || !minContent)) {
            double fr;
            if (definite) {
                double leftover = available - gapsTotal(tracks, gap);
                for (Track t : tracks) if (!t.isFlex() && !t.collapsed) leftover -= t.base;
                fr = findFrSize(flexTracks, leftover);
            } else {
                fr = 0;
                for (Track t : flexTracks) {
                    fr = Math.max(fr, t.flexFactor() > 1 ? t.base / t.flexFactor() : t.base);
                }
                for (Contributor item : items) {
                    if (!spansFlex(tracks, item)) continue;
                    double space = item.maxContribution - gapsWithin(tracks, item, gap);
                    double factorSum = 0;
                    for (int i = item.start; i < item.end(); i++) {
                        Track t = tracks.get(i);
                        if (t.collapsed) continue;
                        if (t.isFlex()) factorSum += t.flexFactor(); else space -= t.base;
                    }
                    if (factorSum < 1) factorSum = 1;
                    fr = Math.max(fr, space / factorSum);
                }
            }
            for (Track t : flexTracks) t.base = Math.max(t.base, fr * t.flexFactor());
        }

        // 5. Stretch auto tracks
        if (definite && stretch) {
            double free = available - gapsTotal(tracks, gap) - baseSum(tracks);
            if (free > EPS) {
                List<Track> autoTracks = new ArrayList<>();
                for (Track t : tracks) if (t.isAutoMax() && !t.collapsed) autoTracks.add(t);
                for (Track t : autoTracks) t.base += free / autoTracks.size();
            }
        }
    }

    private static double findFrSize(List<Track> flexTracks, double leftover) {
        if (leftover <= 0) return 0;
        List<Track> active = new ArrayList<>(flexTracks);
        while (true) {
            double factorSum = 0;
            for (Track t : active) factorSum += t.flexFactor();
            if (factorSum < 1) factorSum = 1;
            double hypothetical = leftover / factorSum;
            boolean restart = false;
            for (int i = active.size() - 1; i >= 0; i--) {
                Track t = active.get(i);
                if (hypothetical * t.flexFactor() < t.base - EPS) {
                    leftover -= t.base;
                    active.remove(i);
                    restart = true;
                }
            }
            if (!restart) return Math.max(0, hypothetical);
            if (active.isEmpty()) return 0;
        }
    }

    private static boolean spansFlex(List<Track> tracks, Contributor item) {
        for (int i = item.start; i < item.end(); i++) if (tracks.get(i).isFlex()) return true;
        return false;
    }

    /**
     * One sub-step of §12.5.1 for a group of items with the same span: computes each item's incurred increase of
     * the affected tracks, keeps the maximum per track, and applies it after the whole group.
     *
     * @param toLimit      grow growth limits instead of base sizes
     * @param useMax       distribute the max-content contribution instead of the minimum contribution
     * @param affected     tracks that receive space
     * @param beyondLimits when growing base sizes: tracks that may grow beyond their limits (all affected if none match)
     */
    private static void distributeSpanGroup(List<Track> tracks, List<Contributor> group, double gap, boolean toLimit, boolean useMax,
                                            java.util.function.Predicate<Track> affected, java.util.function.Predicate<Track> beyondLimits) {
        double[] planned = new double[tracks.size()];
        boolean any = false;
        for (Contributor item : group) {
            double contribution = useMax ? item.maxContribution : item.minContribution;
            double spanned = gapsWithin(tracks, item, gap);
            List<Integer> targets = new ArrayList<>();
            for (int i = item.start; i < item.end(); i++) {
                Track t = tracks.get(i);
                if (t.collapsed) continue;
                spanned += toLimit ? (t.limit == INF ? t.base : t.limit) : t.base;
                if (affected.test(t)) targets.add(i);
            }
            double space = contribution - spanned;
            if (space <= EPS || targets.isEmpty()) continue;
            any = true;
            double[] incurred = new double[tracks.size()];
            if (toLimit) {
                for (int i : targets) incurred[i] = space / targets.size();
            } else {
                double remaining = growUpToLimits(tracks, targets, incurred, space);
                if (remaining > EPS) {
                    List<Integer> unlimited = new ArrayList<>();
                    for (int i : targets) if (beyondLimits.test(tracks.get(i))) unlimited.add(i);
                    if (unlimited.isEmpty()) unlimited = targets;
                    for (int i : unlimited) incurred[i] += remaining / unlimited.size();
                }
            }
            for (int i : targets) planned[i] = Math.max(planned[i], incurred[i]);
        }
        if (!any) return;
        for (int i = 0; i < tracks.size(); i++) {
            if (planned[i] <= 0) continue;
            Track t = tracks.get(i);
            if (toLimit) {
                if (t.limit == INF) t.limit = t.base;
                t.limit += planned[i];
            } else {
                t.base += planned[i];
            }
            if (t.limit < t.base) t.limit = t.base;
        }
    }

    /** Grows the given tracks' increases equally until each reaches its growth limit; returns what could not be placed. */
    private static double growUpToLimits(List<Track> tracks, List<Integer> targets, double[] incurred, double space) {
        List<Integer> open = new ArrayList<>();
        for (int i : targets) if (tracks.get(i).limit > tracks.get(i).base + EPS) open.add(i);
        while (space > EPS && !open.isEmpty()) {
            double share = space / open.size();
            for (int k = open.size() - 1; k >= 0; k--) {
                int i = open.get(k);
                Track t = tracks.get(i);
                double room = t.limit - t.base - incurred[i];
                double add = Math.min(share, room);
                incurred[i] += add;
                space -= add;
                if (room - add <= EPS) open.remove(k);
            }
        }
        return space;
    }

    private static double spannedSize(List<Track> tracks, Contributor item, double gap) {
        double sum = gapsWithin(tracks, item, gap);
        for (int i = item.start; i < item.end(); i++) sum += tracks.get(i).base;
        return sum;
    }

    /** Gap space between the non-collapsed tracks an item spans. */
    static double gapsWithin(List<Track> tracks, Contributor item, double gap) {
        int visible = 0;
        for (int i = item.start; i < item.end(); i++) if (!tracks.get(i).collapsed) visible++;
        return visible > 1 ? gap * (visible - 1) : 0;
    }

    static double gapsTotal(List<Track> tracks, double gap) {
        int visible = visibleCount(tracks);
        return visible > 1 ? gap * (visible - 1) : 0;
    }

    static int visibleCount(List<Track> tracks) {
        int visible = 0;
        for (Track t : tracks) if (!t.collapsed) visible++;
        return visible;
    }

    static double baseSum(List<Track> tracks) {
        double sum = 0;
        for (Track t : tracks) sum += t.base;
        return sum;
    }

    // ── Positioning ───────────────────────────────────────────────────

    /**
     * Computes track start positions according to the content alignment.
     *
     * @return positions of each track's start edge; index {@code tracks.size()} holds the end of the last track
     */
    static double[] positions(List<Track> tracks, double available, double gap, GridContentAlignment alignment) {
        int visible = visibleCount(tracks);
        double total = baseSum(tracks) + gapsTotal(tracks, gap);
        double free = available - total;
        double start = 0;
        double extraGap = 0;
        if (visible > 0) {
            switch (alignment) {
                case END:    start = free; break;
                case CENTER: start = free / 2; break;
                case SPACE_BETWEEN:
                    if (free > 0 && visible > 1) extraGap = free / (visible - 1);
                    break;
                case SPACE_AROUND:
                    if (free > 0) { start = free / (visible * 2); extraGap = free / visible; }
                    else start = free / 2;
                    break;
                case SPACE_EVENLY:
                    if (free > 0) { start = free / (visible + 1); extraGap = free / (visible + 1); }
                    else start = free / 2;
                    break;
                default: break;
            }
        }
        double[] pos = new double[tracks.size() + 1];
        double p = start;
        boolean first = true;
        for (int i = 0; i < tracks.size(); i++) {
            Track t = tracks.get(i);
            if (!t.collapsed) {
                if (!first) p += gap + extraGap;
                first = false;
            }
            pos[i] = p;
            p += t.base;
        }
        pos[tracks.size()] = p;
        return pos;
    }
}
